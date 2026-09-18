package com.vyoog.prospectsoul_backend.verification.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.common.dto.PageResponse;
import com.vyoog.prospectsoul_backend.common.exception.BusinessRuleException;
import com.vyoog.prospectsoul_backend.common.exception.ConflictException;
import com.vyoog.prospectsoul_backend.common.exception.ResourceNotFoundException;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.verification.config.VerificationProperties;
import com.vyoog.prospectsoul_backend.verification.dto.request.StartVerificationRequest;
import com.vyoog.prospectsoul_backend.verification.dto.response.AddedByOptionResponse;
import com.vyoog.prospectsoul_backend.verification.dto.response.EligibleCompanyResponse;
import com.vyoog.prospectsoul_backend.verification.dto.response.VerificationBatchItemResponse;
import com.vyoog.prospectsoul_backend.verification.dto.response.VerificationBatchResponse;
import com.vyoog.prospectsoul_backend.verification.dto.response.VerifiedCompanyResponse;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationBatch;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationBatchItem;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationBatchStatus;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationFailureCode;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationItemStatus;
import com.vyoog.prospectsoul_backend.verification.mapper.VerificationMapper;
import com.vyoog.prospectsoul_backend.verification.repository.VerificationBatchItemRepository;
import com.vyoog.prospectsoul_backend.verification.repository.VerificationBatchRepository;
import com.vyoog.prospectsoul_backend.verification.repository.VerificationCompanyStatsRepository;
import com.vyoog.prospectsoul_backend.verification.specification.VerificationBatchSpecification;
import com.vyoog.prospectsoul_backend.verification.specification.VerificationCompanySpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verification read and batch-creation logic.
 *
 * <p>Per-item processing lives in {@link VerificationItemProcessor} instead,
 * because it must not hold a transaction open across the remote provider call.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> BATCH_SORT_FIELDS = new HashSet<>(Arrays.asList(
            "createdAt", "startedAt", "completedAt", "status", "totalCount",
            "verifiedCount", "failedCount", "skippedCount"));

    private static final Set<String> ITEM_SORT_FIELDS = new HashSet<>(Arrays.asList(
            "createdAt", "completedAt", "startedAt", "status", "lineType", "attemptCount"));

    private static final Set<String> COMPANY_SORT_FIELDS = new HashSet<>(Arrays.asList(
            "canonicalName", "city", "state", "createdAt", "verifiedAt", "verificationStatus"));

    private final VerificationBatchRepository batchRepository;
    private final VerificationBatchItemRepository itemRepository;
    private final CompanyRepository companyRepository;
    private final VerificationCompanyStatsRepository companyStatsRepository;
    private final VerificationMapper mapper;
    private final ActorNameResolver actorNameResolver;
    private final E164PhoneFormatter phoneFormatter;
    private final AuditService auditService;
    private final VerificationProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    // ------------------------------------------------------------------
    // Batch creation
    // ------------------------------------------------------------------

    /**
     * Creates a batch and its queue rows, then returns immediately. No
     * provider call happens on this thread: the caller gets 202 and the worker
     * does the work, so progress does not depend on the browser staying open.
     */
    @Transactional
    public VerificationBatchResponse start(StartVerificationRequest request, String actor) {
        List<UUID> requestedIds = new ArrayList<>(new LinkedHashSet<>(request.companyIds()));
        validateDateRange(request.dateFrom(), request.dateTo());

        if (requestedIds.size() > properties.resolvedMaxBatchCompanies()) {
            throw new BusinessRuleException(
                    "A verification batch may contain at most " + properties.resolvedMaxBatchCompanies()
                            + " companies; " + requestedIds.size() + " were selected");
        }

        List<Company> companies = companyRepository.findAllById(requestedIds);
        if (companies.size() != requestedIds.size()) {
            Set<UUID> found = new HashSet<>();
            companies.forEach(c -> found.add(c.getId()));
            UUID missing = requestedIds.stream().filter(id -> !found.contains(id)).findFirst().orElseThrow();
            throw new ResourceNotFoundException("Company", missing);
        }

        // Invariant: one company is never verified by two batches at once.
        List<UUID> inFlight = itemRepository.findCompanyIdsInFlight(requestedIds);
        if (!inFlight.isEmpty()) {
            throw new ConflictException("These companies are already being verified: " + inFlight);
        }

        Instant createdFrom = startOfDayUtc(request.dateFrom());
        Instant createdToExclusive = exclusiveEndOfDayUtc(request.dateTo());

        List<VerificationBatchItem> items = new ArrayList<>(companies.size());
        int queued = 0;
        int skipped = 0;

        for (Company company : companies) {
            Classification classification = classify(company, request.addedBy(), createdFrom, createdToExclusive);
            VerificationBatchItem item = VerificationBatchItem.builder()
                    .companyId(company.getId())
                    .phoneNumber(company.getPrimaryPhoneNormalized())
                    .status(classification.status())
                    .failureCode(classification.code())
                    .failureMessage(classification.message())
                    .build();

            if (classification.status() == VerificationItemStatus.SKIPPED) {
                item.setCompletedAt(Instant.now());
                skipped++;
            } else {
                item.setNormalizedPhoneNumber(phoneFormatter.toE164(company.getPrimaryPhoneNormalized()));
                queued++;
            }
            items.add(item);
        }

        if (queued == 0) {
            throw new BusinessRuleException(
                    "No eligible companies in the selection: all " + companies.size()
                            + " are already verified, have no usable phone, or fall outside the selected filters");
        }

        VerificationBatch batch = VerificationBatch.builder()
                .requestedBy(actor)
                .status(VerificationBatchStatus.QUEUED)
                .filterAddedBy(blankToNull(request.addedBy()))
                .filterDateFrom(request.dateFrom())
                .filterDateTo(request.dateTo())
                .totalCount(items.size())
                .queuedCount(queued)
                .processingCount(0)
                .verifiedCount(0)
                .failedCount(0)
                .skippedCount(skipped)
                .build();
        VerificationBatch savedBatch = batchRepository.save(batch);

        items.forEach(item -> item.setBatchId(savedBatch.getId()));
        itemRepository.saveAll(items);

        auditService.record("VERIFICATION_BATCH", savedBatch.getId(), actor, "CREATE", null,
                Map.of(
                        "total_count", savedBatch.getTotalCount(),
                        "queued_count", savedBatch.getQueuedCount(),
                        "skipped_count", savedBatch.getSkippedCount(),
                        "filter_added_by", String.valueOf(savedBatch.getFilterAddedBy()),
                        "filter_date_from", String.valueOf(savedBatch.getFilterDateFrom()),
                        "filter_date_to", String.valueOf(savedBatch.getFilterDateTo())));

        eventPublisher.publishEvent(new VerificationBatchStartedEvent(savedBatch.getId()));

        log.info("Verification batch {} created by {}: total={} queued={} skipped={}",
                savedBatch.getId(), actor, savedBatch.getTotalCount(),
                savedBatch.getQueuedCount(), savedBatch.getSkippedCount());

        return toResponse(savedBatch);
    }

    /**
     * Applies the eligibility rules of docs/dev_docs/13 §6 to one company.
     *
     * <p>A company that fails a rule becomes a SKIPPED item rather than
     * disappearing, so the batch's own record explains why every selected
     * company was or was not verified.
     */
    Classification classify(Company company, String addedBy, Instant createdFrom, Instant createdToExclusive) {
        if (company.getPipelineState() == Company.PipelineState.ARCHIVED) {
            return new Classification(VerificationItemStatus.SKIPPED,
                    VerificationFailureCode.NOT_ELIGIBLE, "Company is archived");
        }
        if (company.getVerificationStatus() == Company.VerificationStatus.VERIFIED) {
            return new Classification(VerificationItemStatus.SKIPPED,
                    VerificationFailureCode.ALREADY_VERIFIED, "Company was already verified");
        }
        if (addedBy != null && !addedBy.isBlank() && !addedBy.equals(company.getCreatedBy())) {
            return new Classification(VerificationItemStatus.SKIPPED,
                    VerificationFailureCode.FILTER_MISMATCH,
                    "Company was not added by the selected user");
        }
        if (createdFrom != null && company.getCreatedAt().isBefore(createdFrom)) {
            return new Classification(VerificationItemStatus.SKIPPED,
                    VerificationFailureCode.FILTER_MISMATCH,
                    "Company was added before the selected date range");
        }
        if (createdToExclusive != null && !company.getCreatedAt().isBefore(createdToExclusive)) {
            return new Classification(VerificationItemStatus.SKIPPED,
                    VerificationFailureCode.FILTER_MISMATCH,
                    "Company was added after the selected date range");
        }
        String phone = company.getPrimaryPhoneNormalized();
        if (phone == null || phone.isBlank()) {
            return new Classification(VerificationItemStatus.SKIPPED,
                    VerificationFailureCode.NO_PHONE, "Company has no primary phone");
        }
        if (phoneFormatter.toE164(phone) == null) {
            return new Classification(VerificationItemStatus.SKIPPED,
                    VerificationFailureCode.NO_PHONE,
                    "Primary phone is not a usable number: " + phone);
        }
        return new Classification(VerificationItemStatus.QUEUED, null, null);
    }

    record Classification(VerificationItemStatus status, VerificationFailureCode code, String message) {}

    // ------------------------------------------------------------------
    // Batch reads
    // ------------------------------------------------------------------

    /** The batch the "Current Verification" card renders, if any. */
    @Transactional(readOnly = true)
    public Optional<VerificationBatchResponse> getActive() {
        return batchRepository.findActive().map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public VerificationBatchResponse getById(UUID id) {
        VerificationBatch batch = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Verification batch", id));
        return toResponse(batch);
    }

    @Transactional(readOnly = true)
    public PageResponse<VerificationBatchResponse> list(String status, String requestedBy,
                                                         LocalDate from, LocalDate to,
                                                         int page, int size, String sort, String sortDir) {
        validateDateRange(from, to);
        Pageable pageable = pageable(page, size, sort, sortDir, BATCH_SORT_FIELDS, "createdAt");

        Page<VerificationBatch> result = batchRepository.findAll(
                VerificationBatchSpecification.withFilters(
                        parseBatchStatus(status),
                        blankToNull(requestedBy),
                        startOfDayUtc(from),
                        exclusiveEndOfDayUtc(to)),
                pageable);

        Map<String, String> names = actorNameResolver.resolve(
                result.getContent().stream().flatMap(b -> java.util.stream.Stream.of(
                        b.getRequestedBy(), b.getFilterAddedBy())).toList());

        return PageResponse.from(result.map(batch ->
                mapper.toResponse(batch, names, currentItemOf(batch))));
    }

    @Transactional(readOnly = true)
    public PageResponse<VerificationBatchItemResponse> getItems(UUID batchId, String status, String q,
                                                                 int page, int size,
                                                                 String sort, String sortDir) {
        if (!batchRepository.existsById(batchId)) {
            throw new ResourceNotFoundException("Verification batch", batchId);
        }
        Pageable pageable = pageable(page, size, sort, sortDir, ITEM_SORT_FIELDS, "createdAt");

        Page<VerificationBatchItem> result = itemRepository.findAll(
                VerificationBatchSpecification.itemsOf(batchId, parseItemStatus(status), likePattern(q)),
                pageable);

        Map<UUID, String> companyNames = companyNames(result.getContent().stream()
                .map(VerificationBatchItem::getCompanyId).toList());

        return PageResponse.from(result.map(item ->
                mapper.toResponse(item, companyNames.get(item.getCompanyId()))));
    }

    // ------------------------------------------------------------------
    // Company reads
    // ------------------------------------------------------------------

    /**
     * Selection candidates. Never returns a VERIFIED company: the endpoint
     * exists to populate "Verify New Companies", and offering an already
     * verified company there would only produce an ALREADY_VERIFIED skip.
     */
    @Transactional(readOnly = true)
    public PageResponse<EligibleCompanyResponse> getEligible(String addedBy, LocalDate dateFrom, LocalDate dateTo,
                                                              String verificationStatus, String q,
                                                              int page, int size, String sort, String sortDir) {
        validateDateRange(dateFrom, dateTo);
        Company.VerificationStatus statusFilter = parseEligibleVerificationStatus(verificationStatus);
        Pageable pageable = pageable(page, size, sort, sortDir, COMPANY_SORT_FIELDS, "createdAt");

        Page<Company> result = companyRepository.findAll(
                VerificationCompanySpecification.eligible(
                        statusFilter, blankToNull(addedBy),
                        startOfDayUtc(dateFrom), exclusiveEndOfDayUtc(dateTo),
                        likePattern(q)),
                pageable);

        Map<String, String> names = actorNameResolver.resolve(
                result.getContent().stream().map(Company::getCreatedBy).toList());
        Set<UUID> inFlight = new HashSet<>(itemRepository.findCompanyIdsInFlight(
                result.getContent().stream().map(Company::getId).toList()));

        return PageResponse.from(result.map(company -> mapper.toEligibleResponse(
                company, names,
                phoneFormatter.toE164(company.getPrimaryPhoneNormalized()) != null,
                inFlight.contains(company.getId()))));
    }

    /** The verified-companies table. VERIFIED only, enforced in SQL. */
    @Transactional(readOnly = true)
    public PageResponse<VerifiedCompanyResponse> getVerifiedCompanies(String q, String verifiedBy,
                                                                       LocalDate verifiedFrom, LocalDate verifiedTo,
                                                                       String addedBy,
                                                                       int page, int size,
                                                                       String sort, String sortDir) {
        validateDateRange(verifiedFrom, verifiedTo);
        Pageable pageable = pageable(page, size, sort, sortDir, COMPANY_SORT_FIELDS, "verifiedAt");

        Page<Company> result = companyRepository.findAll(
                VerificationCompanySpecification.verified(
                        likePattern(q), blankToNull(verifiedBy),
                        startOfDayUtc(verifiedFrom), exclusiveEndOfDayUtc(verifiedTo),
                        blankToNull(addedBy)),
                pageable);

        List<UUID> companyIds = result.getContent().stream().map(Company::getId).toList();
        Map<UUID, VerificationBatchItem> latestItems = new HashMap<>();
        if (!companyIds.isEmpty()) {
            for (VerificationBatchItem item : itemRepository.findLatestVerifiedForCompanies(companyIds)) {
                latestItems.putIfAbsent(item.getCompanyId(), item);
            }
        }

        Map<String, String> names = actorNameResolver.resolve(
                result.getContent().stream().flatMap(c -> java.util.stream.Stream.of(
                        c.getVerifiedBy(), c.getCreatedBy())).toList());

        return PageResponse.from(result.map(company ->
                mapper.toVerifiedResponse(company, names, latestItems.get(company.getId()))));
    }

    /**
     * Options for the Added By filter.
     *
     * <p>Derived from the actors that actually appear in
     * {@code companies.created_by} rather than from the user directory: the
     * filter must offer exactly the values it can match, and the user
     * directory is Admin-only.
     */
    @Transactional(readOnly = true)
    public List<AddedByOptionResponse> getAddedByOptions() {
        List<Object[]> rows = companyStatsRepository.countCompaniesByCreator();
        Map<String, String> names = actorNameResolver.resolve(
                rows.stream().map(row -> (String) row[0]).toList());

        return rows.stream()
                .map(row -> {
                    String id = (String) row[0];
                    long count = ((Number) row[1]).longValue();
                    return new AddedByOptionResponse(id, ActorNameResolver.nameOf(names, id), count);
                })
                .toList();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private VerificationBatchResponse toResponse(VerificationBatch batch) {
        Map<String, String> names = actorNameResolver.resolve(
                List.of(nullToEmpty(batch.getRequestedBy()), nullToEmpty(batch.getFilterAddedBy())));
        return mapper.toResponse(batch, names, currentItemOf(batch));
    }

    private VerificationBatchResponse.CurrentItem currentItemOf(VerificationBatch batch) {
        if (batch.getStatus().isTerminal()) {
            return null;
        }
        return itemRepository
                .findFirstByBatchIdAndStatusOrderByStartedAtDesc(batch.getId(), VerificationItemStatus.PROCESSING)
                .map(item -> new VerificationBatchResponse.CurrentItem(
                        item.getCompanyId(),
                        companyNames(List.of(item.getCompanyId())).get(item.getCompanyId()),
                        item.getNormalizedPhoneNumber() != null
                                ? item.getNormalizedPhoneNumber() : item.getPhoneNumber(),
                        item.getProvider()))
                .orElse(null);
    }

    private Map<UUID, String> companyNames(List<UUID> companyIds) {
        if (companyIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> names = new HashMap<>();
        for (Company company : companyRepository.findAllById(companyIds)) {
            names.put(company.getId(), company.getCanonicalName());
        }
        return names;
    }

    private Pageable pageable(int page, int size, String sort, String sortDir,
                               Set<String> allowed, String fallback) {
        int resolvedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String resolvedSort = allowed.contains(sort) ? sort : fallback;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(Math.max(page, 0), resolvedSize, Sort.by(direction, resolvedSort));
    }

    private static void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("date_from must not be after date_to");
        }
    }

    private static VerificationBatchStatus parseBatchStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return VerificationBatchStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown verification batch status: " + status);
        }
    }

    private static VerificationItemStatus parseItemStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return VerificationItemStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown verification item status: " + status);
        }
    }

    private static Company.VerificationStatus parseEligibleVerificationStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        Company.VerificationStatus parsed;
        try {
            parsed = Company.VerificationStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown verification status: " + status);
        }
        if (parsed == Company.VerificationStatus.VERIFIED) {
            throw new IllegalArgumentException(
                    "verification_status=VERIFIED is not a selectable filter here; "
                            + "use GET /api/v1/verifications/companies for verified companies");
        }
        return parsed;
    }

    /** Lower-cased {@code %term%} for the JPQL LIKE filters, or null. */
    private static String likePattern(String term) {
        if (term == null || term.isBlank()) {
            return null;
        }
        return "%" + term.trim().toLowerCase() + "%";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * Inclusive date filters are resolved against UTC day boundaries, matching
     * the project's UTC-everywhere convention ({@code hibernate.jdbc.time_zone:
     * UTC}, ISO-8601 UTC timestamps in JSON).
     */
    private static Instant startOfDayUtc(LocalDate date) {
        return date == null ? null : date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /** Exclusive upper bound, so a {@code date_to} of the 9th includes the 9th. */
    private static Instant exclusiveEndOfDayUtc(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
