package com.vyoog.prospectsoul_backend.verification.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.activity.entity.ActivityType;
import com.vyoog.prospectsoul_backend.activity.service.ActivityService;
import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.mapper.CompanyMapper;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.verification.config.VerificationProperties;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationBatch;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationBatchItem;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationBatchStatus;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationFailureCode;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationItemStatus;
import com.vyoog.prospectsoul_backend.verification.provider.LineTypes;
import com.vyoog.prospectsoul_backend.verification.provider.PhoneVerificationProvider;
import com.vyoog.prospectsoul_backend.verification.provider.PhoneVerificationResult;
import com.vyoog.prospectsoul_backend.verification.repository.VerificationBatchItemRepository;
import com.vyoog.prospectsoul_backend.verification.repository.VerificationBatchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processes one claimed queue item.
 *
 * <p>Split into three steps on purpose: a short read transaction, then the
 * provider call with <em>no</em> transaction open, then a short write
 * transaction. A database transaction must never stay open across a remote
 * call (docs/dev_docs/14 §9) — that is what would turn a slow Twilio response
 * into exhausted connections and a stalled queue.
 */
@Service
@Slf4j
public class VerificationItemProcessor {

    private final VerificationBatchRepository batchRepository;
    private final VerificationBatchItemRepository itemRepository;
    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final PhoneVerificationProvider provider;
    private final ActivityService activityService;
    private final AuditService auditService;
    private final VerificationProperties properties;

    /**
     * Proxy reference to this bean.
     *
     * <p>{@link #process} deliberately calls {@code self.prepare(...)} and
     * {@code self.complete(...)} rather than the methods directly: a plain
     * self-call bypasses Spring's transaction proxy, so
     * {@code @Transactional} would silently not apply and the audit write —
     * which requires an existing transaction — would fail. Same pattern the
     * import pipeline already uses.
     */
    private final VerificationItemProcessor self;

    public VerificationItemProcessor(
            VerificationBatchRepository batchRepository,
            VerificationBatchItemRepository itemRepository,
            CompanyRepository companyRepository,
            CompanyMapper companyMapper,
            PhoneVerificationProvider provider,
            ActivityService activityService,
            AuditService auditService,
            VerificationProperties properties,
            @Lazy VerificationItemProcessor self) {
        this.batchRepository = batchRepository;
        this.itemRepository = itemRepository;
        this.companyRepository = companyRepository;
        this.companyMapper = companyMapper;
        this.provider = provider;
        this.activityService = activityService;
        this.auditService = auditService;
        this.properties = properties;
        this.self = self;
    }

    /**
     * Runs one item to a terminal state, or back to QUEUED when the failure is
     * retryable and the attempt budget is not spent.
     *
     * <p>Never throws for a per-item problem: one bad company must not stop the
     * batch (docs/dev_docs/13 §8).
     */
    public void process(UUID itemId) {
        Preparation preparation;
        try {
            preparation = self.prepare(itemId);
        } catch (RuntimeException e) {
            log.error("verification item={} preparation failed", itemId, e);
            self.failItem(itemId, VerificationFailureCode.PROVIDER_ERROR,
                    "Could not prepare item: " + e.getMessage());
            return;
        }

        if (preparation == null || preparation.terminal()) {
            return;
        }

        PhoneVerificationResult result;
        try {
            // No transaction is open here.
            result = provider.verify(preparation.e164Phone());
        } catch (RuntimeException e) {
            log.error("verification batch={} item={} company={} provider threw unexpectedly",
                    preparation.batchId(), itemId, preparation.companyId(), e);
            result = PhoneVerificationResult.failure(provider.providerName(),
                    VerificationFailureCode.PROVIDER_ERROR,
                    "Provider call failed: " + e.getMessage(), 0);
        }

        try {
            self.complete(itemId, result);
        } catch (RuntimeException e) {
            log.error("verification batch={} item={} could not persist result",
                    preparation.batchId(), itemId, e);
            self.failItem(itemId, VerificationFailureCode.PROVIDER_ERROR,
                    "Could not persist result: " + e.getMessage());
        }
    }

    /**
     * Re-checks eligibility for a claimed item and returns what the provider
     * call needs.
     *
     * <p>Eligibility is re-checked here and not only at batch creation because
     * a company can be verified or edited between the two moments.
     */
    @Transactional
    public Preparation prepare(UUID itemId) {
        VerificationBatchItem item = itemRepository.findById(itemId).orElse(null);
        if (item == null) {
            log.warn("verification item={} vanished before processing", itemId);
            return null;
        }

        Optional<Company> maybeCompany = companyRepository.findById(item.getCompanyId());
        if (maybeCompany.isEmpty()) {
            terminate(item, VerificationItemStatus.FAILED,
                    VerificationFailureCode.NOT_ELIGIBLE, "Company no longer exists");
            return new Preparation(item.getBatchId(), item.getCompanyId(), null, true);
        }

        Company company = maybeCompany.get();
        if (company.getVerificationStatus() == Company.VerificationStatus.VERIFIED) {
            terminate(item, VerificationItemStatus.SKIPPED,
                    VerificationFailureCode.ALREADY_VERIFIED,
                    "Company was verified before this item was processed");
            return new Preparation(item.getBatchId(), item.getCompanyId(), null, true);
        }
        if (company.getPipelineState() == Company.PipelineState.ARCHIVED) {
            terminate(item, VerificationItemStatus.SKIPPED,
                    VerificationFailureCode.NOT_ELIGIBLE, "Company was archived");
            return new Preparation(item.getBatchId(), item.getCompanyId(), null, true);
        }

        String e164 = item.getNormalizedPhoneNumber();
        if (e164 == null || e164.isBlank()) {
            terminate(item, VerificationItemStatus.SKIPPED,
                    VerificationFailureCode.NO_PHONE, "No usable phone number to look up");
            return new Preparation(item.getBatchId(), item.getCompanyId(), null, true);
        }

        item.setProvider(provider.providerName());
        itemRepository.save(item);

        return new Preparation(item.getBatchId(), item.getCompanyId(), e164, false);
    }

    /**
     * Persists the provider result, applies the company mutation, writes the
     * activity and audit rows, and refreshes the batch counters — all in one
     * transaction, so a company can never be marked VERIFIED without its
     * matching activity and audit rows.
     */
    @Transactional
    public void complete(UUID itemId, PhoneVerificationResult result) {
        VerificationBatchItem item = itemRepository.findById(itemId).orElse(null);
        if (item == null) {
            return;
        }

        item.setProvider(result.providerName());
        item.setProviderReference(result.providerReference());
        item.setPhoneValid(result.valid());
        item.setLineType(result.lineType());
        item.setCarrierName(result.carrierName());
        item.setMobileCountryCode(result.mobileCountryCode());
        item.setMobileNetworkCode(result.mobileNetworkCode());
        if (result.phoneNumber() != null) {
            item.setNormalizedPhoneNumber(result.phoneNumber());
        }

        VerificationBatch batch = batchRepository.findById(item.getBatchId()).orElse(null);
        String actor = batch != null ? batch.getRequestedBy() : null;

        Outcome outcome = applyRetryBudget(decide(result), item.getAttemptCount());

        log.info("verification batch={} item={} company={} provider={} attempt={} duration_ms={} outcome={} code={}",
                item.getBatchId(), item.getId(), item.getCompanyId(), result.providerName(),
                item.getAttemptCount(), result.durationMs(), outcome.status(), outcome.code());

        switch (outcome.status()) {
            case VERIFIED -> verifyCompany(item, result, actor);
            case QUEUED -> requeue(item, outcome);
            default -> failWithActivity(item, result, outcome, actor);
        }

        itemRepository.save(item);
        refreshBatch(item.getBatchId(), actor);
    }

    /**
     * Applies the success rule and the retry classification of
     * docs/dev_docs/14 §7 and §10.
     *
     * <p>Success is {@code valid == true AND line type == mobile}. Anything
     * else the provider answered is a permanent failure; a transport-level
     * problem is retried while the attempt budget lasts.
     */
    public Outcome decide(PhoneVerificationResult result) {
        if (result.completed()) {
            if (!Boolean.TRUE.equals(result.valid())) {
                return new Outcome(VerificationItemStatus.FAILED,
                        VerificationFailureCode.INVALID_NUMBER,
                        "Provider reports the number is not valid");
            }
            if (!LineTypes.isMobile(result.lineType())) {
                return new Outcome(VerificationItemStatus.FAILED,
                        VerificationFailureCode.NON_MOBILE_LINE_TYPE,
                        "Line type is '" + (result.lineType() == null ? "unknown" : result.lineType())
                                + "', not mobile");
            }
            return new Outcome(VerificationItemStatus.VERIFIED, null, null);
        }
        return new Outcome(VerificationItemStatus.FAILED, result.failureCode(), result.failureMessage());
    }

    private void verifyCompany(VerificationBatchItem item, PhoneVerificationResult result, String actor) {
        item.setStatus(VerificationItemStatus.VERIFIED);
        item.setFailureCode(null);
        item.setFailureMessage(null);
        item.setCompletedAt(Instant.now());

        Company company = companyRepository.findById(item.getCompanyId()).orElseThrow();
        var previousState = companyMapper.toResponse(company);

        // Canonical verification state stays on the company. The actor recorded
        // is the user who requested the batch — accountability for the decision
        // to verify — while the fact that the check itself was performed
        // automatically by a provider is recorded on the activity and in the
        // VERIFY_AUTOMATED audit action (docs/dev_docs/14 §11).
        company.setVerificationStatus(Company.VerificationStatus.VERIFIED);
        company.setVerifiedBy(actor);
        company.setVerifiedAt(Instant.now());
        company.setUpdatedBy(actor);
        Company saved = companyRepository.save(company);

        activityService.record(item.getCompanyId(), ActivityType.VERIFICATION,
                activityContent(item, result, "VERIFIED", null),
                false, actor);

        auditService.record("COMPANY", saved.getId(), actor, "VERIFY_AUTOMATED",
                previousState, companyMapper.toResponse(saved));
    }

    private void requeue(VerificationBatchItem item, Outcome outcome) {
        item.setStatus(VerificationItemStatus.QUEUED);
        item.setFailureCode(outcome.code());
        item.setFailureMessage(outcome.message() + " — retrying (attempt "
                + item.getAttemptCount() + " of " + properties.resolvedMaxRetries() + ")");
        item.setCompletedAt(null);
    }

    private void failWithActivity(VerificationBatchItem item, PhoneVerificationResult result,
                                   Outcome outcome, String actor) {
        item.setStatus(outcome.status());
        item.setFailureCode(outcome.code());
        item.setFailureMessage(outcome.message());
        item.setCompletedAt(Instant.now());

        // A failed attempt is part of the company's history too: the timeline
        // has to explain why a phone number could not be verified, not just
        // when it could.
        activityService.record(item.getCompanyId(), ActivityType.VERIFICATION,
                activityContent(item, result, outcome.status().name(), outcome),
                false, actor);

        auditService.record("VERIFICATION_BATCH_ITEM", item.getId(), actor, "VERIFY_FAILED", null,
                Map.of(
                        "company_id", String.valueOf(item.getCompanyId()),
                        "status", outcome.status().name(),
                        "failure_code", String.valueOf(outcome.code()),
                        "attempt_count", item.getAttemptCount()));
    }

    /**
     * Turns a permanent-or-transient outcome into the effective one for this
     * attempt: a retryable failure with budget left goes back to QUEUED, a
     * retryable failure with the budget spent becomes
     * {@code MAX_ATTEMPTS_EXCEEDED}, and a permanent failure is untouched.
     * Kept separate from {@link #decide} so the attempt budget is applied in
     * exactly one place.
     */
    public Outcome applyRetryBudget(Outcome outcome, int attemptCount) {
        if (outcome.status() != VerificationItemStatus.FAILED || outcome.code() == null) {
            return outcome;
        }
        if (!outcome.code().retryable()) {
            return outcome;
        }
        if (attemptCount < properties.resolvedMaxRetries()) {
            return new Outcome(VerificationItemStatus.QUEUED, outcome.code(), outcome.message());
        }
        return new Outcome(VerificationItemStatus.FAILED, VerificationFailureCode.MAX_ATTEMPTS_EXCEEDED,
                outcome.message() + " — giving up after " + attemptCount + " attempts");
    }

    private Map<String, Object> activityContent(VerificationBatchItem item, PhoneVerificationResult result,
                                                 String outcome, Outcome failure) {
        Map<String, Object> content = new HashMap<>();
        content.put("method", "PHONE_LOOKUP");
        content.put("automated", true);
        content.put("outcome", outcome);
        content.put("provider", result.providerName());
        content.put("provider_reference", result.providerReference());
        content.put("phone_number", item.getNormalizedPhoneNumber());
        content.put("phone_valid", result.valid());
        content.put("line_type", result.lineType());
        content.put("carrier_name", result.carrierName());
        content.put("mobile_country_code", result.mobileCountryCode());
        content.put("mobile_network_code", result.mobileNetworkCode());
        content.put("batch_id", String.valueOf(item.getBatchId()));
        content.put("item_id", String.valueOf(item.getId()));
        content.put("attempt_count", item.getAttemptCount());
        content.put("duration_ms", result.durationMs());
        // Doc 13 §7: this proves line validity and classification, never that
        // a human answered or that the company owns the number.
        content.put("proves", "PHONE_VALIDITY_AND_LINE_TYPE");
        content.put("ownership_verified", false);
        if (failure != null) {
            content.put("failure_code", String.valueOf(failure.code()));
            content.put("failure_message", failure.message());
        }
        return content;
    }

    /** Marks an item failed outside the normal path, e.g. an unexpected error. */
    @Transactional
    public void failItem(UUID itemId, VerificationFailureCode code, String message) {
        itemRepository.findById(itemId).ifPresent(item -> {
            item.setStatus(VerificationItemStatus.FAILED);
            item.setFailureCode(code);
            item.setFailureMessage(message);
            item.setCompletedAt(Instant.now());
            itemRepository.save(item);
            refreshBatch(item.getBatchId(),
                    batchRepository.findById(item.getBatchId())
                            .map(VerificationBatch::getRequestedBy).orElse(null));
        });
    }

    private void terminate(VerificationBatchItem item, VerificationItemStatus status,
                            VerificationFailureCode code, String message) {
        item.setStatus(status);
        item.setFailureCode(code);
        item.setFailureMessage(message);
        item.setCompletedAt(Instant.now());
        itemRepository.save(item);
        refreshBatch(item.getBatchId(),
                batchRepository.findById(item.getBatchId())
                        .map(VerificationBatch::getRequestedBy).orElse(null));
    }

    /**
     * Recomputes a batch's counters from its items and moves it to the right
     * status.
     *
     * <p>Counters are derived, never incremented: a crash mid-batch, a
     * re-queued retry or a concurrent worker can all skew an incremented
     * counter, whereas a recount from the item rows is always right. This is
     * also what makes restart recovery trivial — the worker recomputes on
     * every pass.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void refreshBatch(UUID batchId, String actor) {
        VerificationBatch batch = batchRepository.findById(batchId).orElse(null);
        if (batch == null) {
            return;
        }

        Map<VerificationItemStatus, Long> counts = new HashMap<>();
        for (Object[] row : itemRepository.countByStatusForBatch(batchId)) {
            counts.put((VerificationItemStatus) row[0], ((Number) row[1]).longValue());
        }

        int queued = intOf(counts.get(VerificationItemStatus.QUEUED));
        int processing = intOf(counts.get(VerificationItemStatus.PROCESSING));
        int verified = intOf(counts.get(VerificationItemStatus.VERIFIED));
        int failed = intOf(counts.get(VerificationItemStatus.FAILED));
        int skipped = intOf(counts.get(VerificationItemStatus.SKIPPED));

        batch.setQueuedCount(queued);
        batch.setProcessingCount(processing);
        batch.setVerifiedCount(verified);
        batch.setFailedCount(failed);
        batch.setSkippedCount(skipped);

        VerificationBatchStatus previousStatus = batch.getStatus();

        if (previousStatus == VerificationBatchStatus.CANCELLED) {
            batchRepository.save(batch);
            return;
        }

        if (queued == 0 && processing == 0) {
            batch.setStatus(failed > 0
                    ? VerificationBatchStatus.COMPLETED_WITH_ERRORS
                    : VerificationBatchStatus.COMPLETED);
            if (batch.getStartedAt() == null) {
                batch.setStartedAt(batch.getCreatedAt());
            }
            if (batch.getCompletedAt() == null) {
                batch.setCompletedAt(Instant.now());
            }
        } else if (processing > 0 || verified > 0 || failed > 0) {
            // Work has actually begun. Skipped items alone do not count: they
            // are decided at creation time, before the worker ever runs.
            batch.setStatus(VerificationBatchStatus.PROCESSING);
            batch.setCompletedAt(null);
            if (batch.getStartedAt() == null) {
                batch.setStartedAt(Instant.now());
            }
        } else {
            batch.setStatus(VerificationBatchStatus.QUEUED);
            batch.setCompletedAt(null);
        }

        VerificationBatch saved = batchRepository.save(batch);

        if (previousStatus != saved.getStatus() && saved.getStatus().isTerminal()) {
            auditService.record("VERIFICATION_BATCH", saved.getId(), actor, "COMPLETE",
                    Map.of("status", previousStatus.name()),
                    Map.of(
                            "status", saved.getStatus().name(),
                            "total_count", saved.getTotalCount(),
                            "verified_count", saved.getVerifiedCount(),
                            "failed_count", saved.getFailedCount(),
                            "skipped_count", saved.getSkippedCount()));
            log.info("verification batch={} finished status={} verified={} failed={} skipped={}",
                    saved.getId(), saved.getStatus(), saved.getVerifiedCount(),
                    saved.getFailedCount(), saved.getSkippedCount());
        }
    }

    public record Preparation(UUID batchId, UUID companyId, String e164Phone, boolean terminal) {}

    public record Outcome(VerificationItemStatus status, VerificationFailureCode code, String message) {}

    private static int intOf(Long value) {
        return value == null ? 0 : value.intValue();
    }
}
