package com.vyoog.prospectsoul_backend.verification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.TestcontainersConfiguration;
import com.vyoog.prospectsoul_backend.activity.entity.Activity;
import com.vyoog.prospectsoul_backend.activity.entity.ActivityType;
import com.vyoog.prospectsoul_backend.activity.repository.ActivityRepository;
import com.vyoog.prospectsoul_backend.common.audit.entity.AuditLog;
import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.verification.dto.request.StartVerificationRequest;
import com.vyoog.prospectsoul_backend.verification.dto.response.VerificationBatchResponse;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationBatch;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationBatchItem;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationBatchStatus;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationFailureCode;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationItemStatus;
import com.vyoog.prospectsoul_backend.verification.repository.VerificationBatchItemRepository;
import com.vyoog.prospectsoul_backend.verification.repository.VerificationBatchRepository;
import com.vyoog.prospectsoul_backend.verification.service.VerificationService;
import com.vyoog.prospectsoul_backend.verification.worker.VerificationWorker;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The Verify vertical slice end to end against a real PostgreSQL:
 * select → start → worker claim → provider → company state → activity →
 * audit → history.
 *
 * <p>The provider is {@link StubPhoneVerificationProvider} and the scheduled
 * poll is disabled, so the worker is driven with explicit {@code runOnce()}
 * calls. Every assertion therefore happens at a known point instead of racing
 * a background thread — and no test can reach Twilio.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, VerificationTestConfiguration.class})
class VerificationIntegrationTest {

    private static final String ANALYST = "a1000000-0000-0000-0000-000000000001";
    private static final String ANALYST_2 = "a1000000-0000-0000-0000-000000000002";
    private static final String SALES_LEAD = "b2000000-0000-0000-0000-000000000001";

    @Autowired private VerificationService verificationService;
    @Autowired private VerificationWorker worker;
    @Autowired private VerificationBatchRepository batchRepository;
    @Autowired private VerificationBatchItemRepository itemRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private ActivityRepository activityRepository;
    @Autowired private AuditService auditService;
    @Autowired private StubPhoneVerificationProvider provider;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetProvider() {
        provider.reset();
    }

    // ------------------------------------------------------------------
    // Batch creation and eligibility
    // ------------------------------------------------------------------

    @Test
    void start_returnsAQueuedBatchWithoutProcessingAnything() {
        Company company = company("Start Corp", "9876500001", ANALYST);
        provider.script("+919876500001", StubPhoneVerificationProvider.mobile("+919876500001", "Airtel"));

        VerificationBatchResponse batch = verificationService.start(
                new StartVerificationRequest(List.of(company.getId()), null, null, null), ANALYST);

        assertThat(batch.status()).isEqualTo("QUEUED");
        assertThat(batch.totalCount()).isEqualTo(1);
        assertThat(batch.queuedCount()).isEqualTo(1);
        assertThat(batch.verifiedCount()).isZero();
        assertThat(batch.requestedBy()).isEqualTo(ANALYST);
        assertThat(batch.requestedByName()).isEqualTo("Priya Sharma");
        // Nothing has been looked up yet: 202 means "accepted", not "done".
        assertThat(provider.calls()).isEmpty();
        assertThat(reload(company).getVerificationStatus())
                .isEqualTo(Company.VerificationStatus.UNVERIFIED);
    }

    @Test
    void start_addedByFilterIsReappliedServerSide() {
        Company mine = company("Mine Corp", "9876500010", ANALYST);
        Company theirs = company("Theirs Corp", "9876500011", ANALYST_2);

        VerificationBatchResponse batch = verificationService.start(
                new StartVerificationRequest(List.of(mine.getId(), theirs.getId()),
                        ANALYST, null, null), ANALYST);

        // Passing an id that does not match the filter cannot smuggle it in.
        assertThat(batch.queuedCount()).isEqualTo(1);
        assertThat(batch.skippedCount()).isEqualTo(1);
        assertThat(itemFor(batch.id(), theirs.getId()).getFailureCode())
                .isEqualTo(VerificationFailureCode.FILTER_MISMATCH);
        assertThat(itemFor(batch.id(), mine.getId()).getStatus())
                .isEqualTo(VerificationItemStatus.QUEUED);
    }

    @Test
    void start_dateRangeIsInclusiveOnBothEnds() {
        Instant dayStart = LocalDate.now().atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        Company onFirstInstant = companyCreatedAt("Edge Start Corp", "9876500020", ANALYST, dayStart);
        Company onLastInstant = companyCreatedAt("Edge End Corp", "9876500021", ANALYST,
                dayStart.plus(1, ChronoUnit.DAYS).minusMillis(1));
        Company dayBefore = companyCreatedAt("Too Early Corp", "9876500022", ANALYST,
                dayStart.minusMillis(1));

        LocalDate today = LocalDate.now();
        VerificationBatchResponse batch = verificationService.start(
                new StartVerificationRequest(
                        List.of(onFirstInstant.getId(), onLastInstant.getId(), dayBefore.getId()),
                        null, today, today), ANALYST);

        assertThat(batch.queuedCount()).isEqualTo(2);
        assertThat(itemFor(batch.id(), dayBefore.getId()).getFailureCode())
                .isEqualTo(VerificationFailureCode.FILTER_MISMATCH);
    }

    @Test
    void start_alreadyVerifiedCompanyIsSkippedNotVerifiedAgain() {
        Company verified = company("Already Verified Corp", "9876500030", ANALYST);
        verified.setVerificationStatus(Company.VerificationStatus.VERIFIED);
        verified.setVerifiedBy(SALES_LEAD);
        verified.setVerifiedAt(Instant.now());
        companyRepository.save(verified);
        Company pending = company("Pending Corp", "9876500031", ANALYST);

        VerificationBatchResponse batch = verificationService.start(
                new StartVerificationRequest(List.of(verified.getId(), pending.getId()),
                        null, null, null), ANALYST);

        assertThat(itemFor(batch.id(), verified.getId()).getStatus())
                .isEqualTo(VerificationItemStatus.SKIPPED);
        assertThat(itemFor(batch.id(), verified.getId()).getFailureCode())
                .isEqualTo(VerificationFailureCode.ALREADY_VERIFIED);
    }

    @Test
    void start_companyWithoutAUsablePhoneIsSkippedWithNoPhone() {
        Company noPhone = company("No Phone Corp", null, ANALYST);
        Company badPhone = company("Bad Phone Corp", "12345", ANALYST);
        Company good = company("Good Phone Corp", "9876500040", ANALYST);

        VerificationBatchResponse batch = verificationService.start(
                new StartVerificationRequest(
                        List.of(noPhone.getId(), badPhone.getId(), good.getId()),
                        null, null, null), ANALYST);

        assertThat(batch.skippedCount()).isEqualTo(2);
        assertThat(itemFor(batch.id(), noPhone.getId()).getFailureCode())
                .isEqualTo(VerificationFailureCode.NO_PHONE);
        assertThat(itemFor(batch.id(), badPhone.getId()).getFailureCode())
                .isEqualTo(VerificationFailureCode.NO_PHONE);
    }

    @Test
    void start_archivedCompanyIsNotEligible() {
        Company archived = company("Archived Corp", "9876500050", ANALYST);
        archived.setPipelineState(Company.PipelineState.ARCHIVED);
        companyRepository.save(archived);
        Company active = company("Active Corp", "9876500051", ANALYST);

        VerificationBatchResponse batch = verificationService.start(
                new StartVerificationRequest(List.of(archived.getId(), active.getId()),
                        null, null, null), ANALYST);

        assertThat(itemFor(batch.id(), archived.getId()).getFailureCode())
                .isEqualTo(VerificationFailureCode.NOT_ELIGIBLE);
    }

    @Test
    void start_withNoEligibleCompany_is422AndCreatesNoBatch() {
        Company noPhone = company("Nothing Eligible Corp", null, ANALYST);
        long batchesBefore = batchRepository.count();

        assertThatThrownBy(() -> verificationService.start(
                new StartVerificationRequest(List.of(noPhone.getId()), null, null, null), ANALYST))
                .isInstanceOf(com.vyoog.prospectsoul_backend.common.exception.BusinessRuleException.class)
                .hasMessageContaining("No eligible companies");

        assertThat(batchRepository.count()).isEqualTo(batchesBefore);
    }

    @Test
    void start_companyAlreadyInFlight_is409() {
        Company company = company("In Flight Corp", "9876500060", ANALYST);
        verificationService.start(
                new StartVerificationRequest(List.of(company.getId()), null, null, null), ANALYST);

        // The first batch's item is still QUEUED, so a second batch must be refused.
        assertThatThrownBy(() -> verificationService.start(
                new StartVerificationRequest(List.of(company.getId()), null, null, null), SALES_LEAD))
                .isInstanceOf(com.vyoog.prospectsoul_backend.common.exception.ConflictException.class);
    }

    @Test
    void start_duplicateIdsInOneRequestProduceOneItem() {
        Company company = company("Dedupe Corp", "9876500070", ANALYST);

        VerificationBatchResponse batch = verificationService.start(
                new StartVerificationRequest(
                        List.of(company.getId(), company.getId(), company.getId()),
                        null, null, null), ANALYST);

        assertThat(batch.totalCount()).isEqualTo(1);
        assertThat(itemRepository.findByBatchId(batch.id())).hasSize(1);
    }

    @Test
    void start_unknownCompanyId_is404() {
        assertThatThrownBy(() -> verificationService.start(
                new StartVerificationRequest(List.of(UUID.randomUUID()), null, null, null), ANALYST))
                .isInstanceOf(com.vyoog.prospectsoul_backend.common.exception.ResourceNotFoundException.class);
    }

    @Test
    void start_invertedDateRange_is400() {
        Company company = company("Bad Range Corp", "9876500080", ANALYST);

        assertThatThrownBy(() -> verificationService.start(
                new StartVerificationRequest(List.of(company.getId()), null,
                        LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 1)), ANALYST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("date_from must not be after date_to");
    }

    @Test
    void start_writesAnAuditRowForTheBatch() {
        Company company = company("Audited Batch Corp", "9876500090", ANALYST);

        VerificationBatchResponse batch = verificationService.start(
                new StartVerificationRequest(List.of(company.getId()), ANALYST, null, null), ANALYST);

        List<AuditLog> audit = auditService.getAuditHistory("VERIFICATION_BATCH", batch.id());
        assertThat(audit).hasSize(1);
        assertThat(audit.getFirst().getAction()).isEqualTo("CREATE");
        assertThat(audit.getFirst().getActor()).isEqualTo(ANALYST);
        assertThat(audit.getFirst().getNewState()).contains("filter_added_by");
    }

    // ------------------------------------------------------------------
    // Worker: success, failure, retry
    // ------------------------------------------------------------------

    @Test
    void worker_validMobile_verifiesTheCompanyAndClosesTheBatch() {
        Company company = company("Verified Corp", "9876510001", ANALYST);
        provider.script("+919876510001",
                StubPhoneVerificationProvider.mobile("+919876510001", "Airtel"));

        VerificationBatchResponse created = verificationService.start(
                new StartVerificationRequest(List.of(company.getId()), null, null, null), ANALYST);
        drain();

        VerificationBatchItem item = itemFor(created.id(), company.getId());
        assertThat(item.getStatus()).isEqualTo(VerificationItemStatus.VERIFIED);
        assertThat(item.getPhoneValid()).isTrue();
        assertThat(item.getLineType()).isEqualTo("mobile");
        assertThat(item.getCarrierName()).isEqualTo("Airtel");
        assertThat(item.getMobileCountryCode()).isEqualTo("404");
        assertThat(item.getMobileNetworkCode()).isEqualTo("90");
        assertThat(item.getProvider()).isEqualTo("stub");
        assertThat(item.getProviderReference()).isEqualTo("https://stub/lookup/+919876510001");
        assertThat(item.getNormalizedPhoneNumber()).isEqualTo("+919876510001");
        assertThat(item.getAttemptCount()).isEqualTo(1);
        assertThat(item.getCompletedAt()).isNotNull();

        Company verified = reload(company);
        assertThat(verified.getVerificationStatus()).isEqualTo(Company.VerificationStatus.VERIFIED);
        assertThat(verified.getVerifiedBy()).isEqualTo(ANALYST);
        assertThat(verified.getVerifiedAt()).isNotNull();

        VerificationBatch batch = batchRepository.findById(created.id()).orElseThrow();
        assertThat(batch.getStatus()).isEqualTo(VerificationBatchStatus.COMPLETED);
        assertThat(batch.getVerifiedCount()).isEqualTo(1);
        assertThat(batch.getQueuedCount()).isZero();
        assertThat(batch.getProcessingCount()).isZero();
        assertThat(batch.getStartedAt()).isNotNull();
        assertThat(batch.getCompletedAt()).isNotNull();
    }

    @Test
    void worker_invalidNumber_failsTheItemAndLeavesTheCompanyUnverified() {
        Company company = company("Invalid Number Corp", "9876510010", ANALYST);
        provider.script("+919876510010", StubPhoneVerificationProvider.invalid("+919876510010"));

        VerificationBatchResponse created = verificationService.start(
                new StartVerificationRequest(List.of(company.getId()), null, null, null), ANALYST);
        drain();

        VerificationBatchItem item = itemFor(created.id(), company.getId());
        assertThat(item.getStatus()).isEqualTo(VerificationItemStatus.FAILED);
        assertThat(item.getFailureCode()).isEqualTo(VerificationFailureCode.INVALID_NUMBER);
        assertThat(item.getPhoneValid()).isFalse();

        assertThat(reload(company).getVerificationStatus())
                .isEqualTo(Company.VerificationStatus.UNVERIFIED);
        assertThat(reload(company).getVerifiedAt()).isNull();

        assertThat(batchRepository.findById(created.id()).orElseThrow().getStatus())
                .isEqualTo(VerificationBatchStatus.COMPLETED_WITH_ERRORS);
    }

    @Test
    void worker_nonMobileLineType_failsAndDoesNotVerify() {
        Company company = company("Landline Corp", "9876510020", ANALYST);
        provider.script("+919876510020",
                StubPhoneVerificationProvider.lineType("+919876510020", "landline"));

        VerificationBatchResponse created = verificationService.start(
                new StartVerificationRequest(List.of(company.getId()), null, null, null), ANALYST);
        drain();

        VerificationBatchItem item = itemFor(created.id(), company.getId());
        assertThat(item.getStatus()).isEqualTo(VerificationItemStatus.FAILED);
        assertThat(item.getFailureCode()).isEqualTo(VerificationFailureCode.NON_MOBILE_LINE_TYPE);
        assertThat(item.getLineType()).isEqualTo("landline");
        assertThat(item.getPhoneValid()).isTrue();

        assertThat(reload(company).getVerificationStatus())
                .isEqualTo(Company.VerificationStatus.UNVERIFIED);
    }

    @Test
    void worker_transientFailureIsRetriedThenSucceeds() {
        Company company = company("Retry Corp", "9876510030", ANALYST);
        provider.scriptByAttempt("+919876510030", attempt -> attempt < 3
                ? StubPhoneVerificationProvider.failure(
                        VerificationFailureCode.PROVIDER_TIMEOUT, "read timed out")
                : StubPhoneVerificationProvider.mobile("+919876510030", "Jio"));

        VerificationBatchResponse created = verificationService.start(
                new StartVerificationRequest(List.of(company.getId()), null, null, null), ANALYST);
        drain();

        assertThat(provider.callCount("+919876510030")).isEqualTo(3);
        VerificationBatchItem item = itemFor(created.id(), company.getId());
        assertThat(item.getStatus()).isEqualTo(VerificationItemStatus.VERIFIED);
        assertThat(item.getAttemptCount()).isEqualTo(3);
        assertThat(reload(company).getVerificationStatus())
                .isEqualTo(Company.VerificationStatus.VERIFIED);
    }

    @Test
    void worker_transientFailureExhaustsTheBudgetAndStops() {
        Company company = company("Always Timeout Corp", "9876510040", ANALYST);
        provider.script("+919876510040", StubPhoneVerificationProvider.failure(
                VerificationFailureCode.RATE_LIMITED, "429 Too Many Requests"));

        VerificationBatchResponse created = verificationService.start(
                new StartVerificationRequest(List.of(company.getId()), null, null, null), ANALYST);
        drain();

        // maxRetries = 3 in src/test/resources/application.yaml.
        assertThat(provider.callCount("+919876510040")).isEqualTo(3);
        VerificationBatchItem item = itemFor(created.id(), company.getId());
        assertThat(item.getStatus()).isEqualTo(VerificationItemStatus.FAILED);
        assertThat(item.getFailureCode()).isEqualTo(VerificationFailureCode.MAX_ATTEMPTS_EXCEEDED);
        assertThat(item.getAttemptCount()).isEqualTo(3);
    }

    @Test
    void worker_permanentProviderErrorIsNotRetried() {
        Company company = company("Auth Error Corp", "9876510050", ANALYST);
        provider.script("+919876510050", StubPhoneVerificationProvider.failure(
                VerificationFailureCode.PROVIDER_AUTH_ERROR, "HTTP 401"));

        VerificationBatchResponse created = verificationService.start(
                new StartVerificationRequest(List.of(company.getId()), null, null, null), ANALYST);
        drain();

        assertThat(provider.callCount("+919876510050")).isEqualTo(1);
        assertThat(itemFor(created.id(), company.getId()).getFailureCode())
                .isEqualTo(VerificationFailureCode.PROVIDER_AUTH_ERROR);
    }

    @Test
    void worker_oneFailingCompanyDoesNotStopTheBatch() {
        Company good1 = company("Batch Good One", "9876520001", ANALYST);
        Company bad = company("Batch Bad", "9876520002", ANALYST);
        Company good2 = company("Batch Good Two", "9876520003", ANALYST);
        provider.script("+919876520001", StubPhoneVerificationProvider.mobile("+919876520001", "Airtel"));
        provider.script("+919876520002", StubPhoneVerificationProvider.invalid("+919876520002"));
        provider.script("+919876520003", StubPhoneVerificationProvider.mobile("+919876520003", "Vi"));

        VerificationBatchResponse created = verificationService.start(
                new StartVerificationRequest(
                        List.of(good1.getId(), bad.getId(), good2.getId()), null, null, null), ANALYST);
        drain();

        VerificationBatch batch = batchRepository.findById(created.id()).orElseThrow();
        assertThat(batch.getVerifiedCount()).isEqualTo(2);
        assertThat(batch.getFailedCount()).isEqualTo(1);
        assertThat(batch.getStatus()).isEqualTo(VerificationBatchStatus.COMPLETED_WITH_ERRORS);
        assertThat(reload(good1).getVerificationStatus()).isEqualTo(Company.VerificationStatus.VERIFIED);
        assertThat(reload(good2).getVerificationStatus()).isEqualTo(Company.VerificationStatus.VERIFIED);
        assertThat(reload(bad).getVerificationStatus()).isEqualTo(Company.VerificationStatus.UNVERIFIED);
    }

    @Test
    void worker_processesALargeBatchWithoutLosingAnyItem() {
        int size = 100;
        List<UUID> ids = new java.util.ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String phone = String.format("98765%05d", 30000 + i);
            Company company = company("Bulk Corp " + i, phone, ANALYST);
            ids.add(company.getId());
            // Every tenth number is a landline, so the batch mixes outcomes.
            provider.script("+91" + phone, i % 10 == 0
                    ? StubPhoneVerificationProvider.lineType("+91" + phone, "landline")
                    : StubPhoneVerificationProvider.mobile("+91" + phone, "Airtel"));
        }

        VerificationBatchResponse created = verificationService.start(
                new StartVerificationRequest(ids, null, null, null), ANALYST);
        assertThat(created.totalCount()).isEqualTo(size);
        drain();

        VerificationBatch batch = batchRepository.findById(created.id()).orElseThrow();
        assertThat(batch.getVerifiedCount()).isEqualTo(90);
        assertThat(batch.getFailedCount()).isEqualTo(10);
        assertThat(batch.getQueuedCount()).isZero();
        assertThat(batch.getProcessingCount()).isZero();
        assertThat(batch.getVerifiedCount() + batch.getFailedCount() + batch.getSkippedCount())
                .isEqualTo(size);
        assertThat(batch.getStatus()).isEqualTo(VerificationBatchStatus.COMPLETED_WITH_ERRORS);
    }

    // ------------------------------------------------------------------
    // Persistence, progress and restart recovery
    // ------------------------------------------------------------------

    @Test
    void activeBatch_isReadBackFromTheDatabase() {
        Company a = company("Active One", "9876540001", ANALYST);
        Company b = company("Active Two", "9876540002", ANALYST);
        provider.script("+919876540001", StubPhoneVerificationProvider.mobile("+919876540001", "Airtel"));
        provider.script("+919876540002", StubPhoneVerificationProvider.mobile("+919876540002", "Airtel"));

        VerificationBatchResponse created = verificationService.start(
                new StartVerificationRequest(List.of(a.getId(), b.getId()), null, null, null), ANALYST);

        // Nothing in memory is consulted: this is the same read the browser
        // makes after a refresh or a restart.
        VerificationBatchResponse active = verificationService.getActive().orElseThrow();
        assertThat(active.id()).isEqualTo(created.id());
        assertThat(active.status()).isEqualTo("QUEUED");
        assertThat(active.progressPercent()).isZero();

        drain();

        assertThat(verificationService.getActive()).isEmpty();
        VerificationBatchResponse finished = verificationService.getById(created.id());
        assertThat(finished.status()).isEqualTo("COMPLETED");
        assertThat(finished.progressPercent()).isEqualTo(100);
        assertThat(finished.elapsedSeconds()).isNotNull();
    }

    @Test
    void worker_recoversItemsAbandonedByADeadJvm() {
        Company company = company("Recovery Corp", "9876550001", ANALYST);
        provider.script("+919876550001", StubPhoneVerificationProvider.mobile("+919876550001", "Airtel"));

        VerificationBatchResponse created = verificationService.start(
                new StartVerificationRequest(List.of(company.getId()), null, null, null), ANALYST);

        // Simulate a crash between claim and result: the item is left
        // PROCESSING with a started_at older than the stale timeout.
        VerificationBatchItem item = itemFor(created.id(), company.getId());
        item.setStatus(VerificationItemStatus.PROCESSING);
        item.setStartedAt(Instant.now().minusSeconds(600));
        item.setAttemptCount(1);
        itemRepository.save(item);

        assertThat(worker.recoverAbandonedItems()).isEqualTo(1);
        assertThat(itemRepository.findById(item.getId()).orElseThrow().getStatus())
                .isEqualTo(VerificationItemStatus.QUEUED);

        drain();

        assertThat(itemFor(created.id(), company.getId()).getStatus())
                .isEqualTo(VerificationItemStatus.VERIFIED);
        assertThat(reload(company).getVerificationStatus())
                .isEqualTo(Company.VerificationStatus.VERIFIED);
        assertThat(batchRepository.findById(created.id()).orElseThrow().getStatus())
                .isEqualTo(VerificationBatchStatus.COMPLETED);
    }

    @Test
    void worker_reconcilesABatchWhoseCountersDriftedBeforeACrash() {
        Company company = company("Reconcile Corp", "9876550010", ANALYST);
        provider.script("+919876550010", StubPhoneVerificationProvider.mobile("+919876550010", "Airtel"));

        VerificationBatchResponse created = verificationService.start(
                new StartVerificationRequest(List.of(company.getId()), null, null, null), ANALYST);
        drain();

        // Corrupt the batch the way an interrupted counter update would.
        VerificationBatch batch = batchRepository.findById(created.id()).orElseThrow();
        batch.setStatus(VerificationBatchStatus.PROCESSING);
        batch.setVerifiedCount(0);
        batch.setQueuedCount(1);
        batch.setCompletedAt(null);
        batchRepository.save(batch);

        worker.runOnce();

        VerificationBatch reconciled = batchRepository.findById(created.id()).orElseThrow();
        assertThat(reconciled.getStatus()).isEqualTo(VerificationBatchStatus.COMPLETED);
        assertThat(reconciled.getVerifiedCount()).isEqualTo(1);
        assertThat(reconciled.getQueuedCount()).isZero();
        assertThat(reconciled.getCompletedAt()).isNotNull();
    }

    // ------------------------------------------------------------------
    // Activity, audit and timeline composition
    // ------------------------------------------------------------------

    @Test
    void successfulVerification_writesAVerificationActivityAndAnAuditRow() {
        Company company = company("Timeline Corp", "9876560001", ANALYST);
        provider.script("+919876560001", StubPhoneVerificationProvider.mobile("+919876560001", "Airtel"));

        verificationService.start(
                new StartVerificationRequest(List.of(company.getId()), null, null, null), ANALYST);
        drain();

        List<Activity> activities = activityRepository
                .findByCompanyIdAndTypeOrderByCreatedAtDesc(company.getId(), ActivityType.VERIFICATION);
        assertThat(activities).hasSize(1);
        Activity activity = activities.getFirst();
        assertThat(activity.getCreatedBy()).isEqualTo(ANALYST);
        // An automated provider check is never human-verified evidence.
        assertThat(activity.getVerified()).isFalse();
        // The column is jsonb, so PostgreSQL rewrites key order and spacing on
        // read; assert on the parsed content rather than the raw text.
        var content = contentOf(activity);
        assertThat(content).containsEntry("outcome", "VERIFIED");
        assertThat(content).containsEntry("line_type", "mobile");
        assertThat(content).containsEntry("carrier_name", "Airtel");
        assertThat(content).containsEntry("provider", "stub");
        assertThat(content).containsEntry("automated", true);
        assertThat(content).containsEntry("ownership_verified", false);
        assertThat(content).containsEntry("proves", "PHONE_VALIDITY_AND_LINE_TYPE");
        assertThat(content).containsEntry("phone_number", "+919876560001");

        List<AuditLog> audit = auditService.getAuditHistory("COMPANY", company.getId());
        assertThat(audit).anyMatch(row -> "VERIFY_AUTOMATED".equals(row.getAction())
                && ANALYST.equals(row.getActor()));
    }

    @Test
    void failedVerification_alsoAppearsOnTheTimeline() {
        Company company = company("Failed Timeline Corp", "9876560010", ANALYST);
        provider.script("+919876560010",
                StubPhoneVerificationProvider.lineType("+919876560010", "fixedVoip"));

        verificationService.start(
                new StartVerificationRequest(List.of(company.getId()), null, null, null), ANALYST);
        drain();

        List<Activity> activities = activityRepository
                .findByCompanyIdAndTypeOrderByCreatedAtDesc(company.getId(), ActivityType.VERIFICATION);
        assertThat(activities).hasSize(1);
        var content = contentOf(activities.getFirst());
        assertThat(content).containsEntry("outcome", "FAILED");
        assertThat(content).containsEntry("failure_code", "NON_MOBILE_LINE_TYPE");
        assertThat(content).containsEntry("line_type", "fixedvoip");
        assertThat(content).containsEntry("phone_valid", true);
    }

    @Test
    void verificationActivitiesComposeIntoTheCompanyTimelineInOrder() {
        Company company = company("Two Attempt Corp", "9876560020", ANALYST);
        provider.script("+919876560020", StubPhoneVerificationProvider.invalid("+919876560020"));

        verificationService.start(
                new StartVerificationRequest(List.of(company.getId()), null, null, null), ANALYST);
        drain();

        provider.reset();
        provider.script("+919876560020",
                StubPhoneVerificationProvider.mobile("+919876560020", "Airtel"));
        verificationService.start(
                new StartVerificationRequest(List.of(company.getId()), null, null, null), ANALYST);
        drain();

        // The timeline is composed from the real tables; there is no second
        // event store to drift out of sync.
        List<Activity> timeline = activityRepository
                .findByCompanyIdOrderByCreatedAtDesc(company.getId());
        assertThat(timeline).hasSize(2);
        assertThat(contentOf(timeline.getFirst())).containsEntry("outcome", "VERIFIED");
        assertThat(contentOf(timeline.getLast())).containsEntry("outcome", "FAILED");
    }

    // ------------------------------------------------------------------
    // Reads: eligible, verified, history, items
    // ------------------------------------------------------------------

    @Test
    void eligible_excludesVerifiedCompaniesAndFlagsInFlightOnes() {
        Company unverified = company("Eligible Unverified", "9876570001", ANALYST);
        Company alreadyVerified = company("Eligible Verified", "9876570002", ANALYST);
        alreadyVerified.setVerificationStatus(Company.VerificationStatus.VERIFIED);
        alreadyVerified.setVerifiedAt(Instant.now());
        alreadyVerified.setVerifiedBy(ANALYST);
        companyRepository.save(alreadyVerified);

        var page = verificationService.getEligible(ANALYST, null, null, null,
                "Eligible", 0, 50, "createdAt", "desc");

        assertThat(page.content()).extracting("id").contains(unverified.getId());
        assertThat(page.content()).extracting("id").doesNotContain(alreadyVerified.getId());
        assertThat(page.content()).allMatch(row -> !"VERIFIED".equals(row.verificationStatus()));
    }

    @Test
    void eligible_marksInFlightCompaniesSoTheyCannotBeSelectedTwice() {
        Company company = company("In Flight Flag Corp", "9876570010", ANALYST);
        verificationService.start(
                new StartVerificationRequest(List.of(company.getId()), null, null, null), ANALYST);

        var page = verificationService.getEligible(null, null, null, null,
                "In Flight Flag Corp", 0, 25, "createdAt", "desc");

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().inFlight()).isTrue();
    }

    @Test
    void eligible_flagsUnusablePhonesUpFront() {
        company("Unusable Phone Corp", "12345", ANALYST);

        var page = verificationService.getEligible(null, null, null, null,
                "Unusable Phone Corp", 0, 25, "createdAt", "desc");

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().phoneUsable()).isFalse();
    }

    @Test
    void eligible_includesInvalidatedCompanies() {
        Company invalidated = company("Invalidated Corp", "9876570020", ANALYST);
        invalidated.setVerificationStatus(Company.VerificationStatus.INVALIDATED);
        companyRepository.save(invalidated);

        var page = verificationService.getEligible(null, null, null, "INVALIDATED",
                "Invalidated Corp", 0, 25, "createdAt", "desc");

        assertThat(page.content()).extracting("id").contains(invalidated.getId());
    }

    @Test
    void eligible_rejectsVerifiedAsAStatusFilter() {
        assertThatThrownBy(() -> verificationService.getEligible(null, null, null, "VERIFIED",
                null, 0, 25, "createdAt", "desc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verifiedCompanies_neverIncludeAnUnverifiedCompany() {
        Company willVerify = company("Table Verified Corp", "9876580001", ANALYST);
        Company willFail = company("Table Failed Corp", "9876580002", ANALYST);
        provider.script("+919876580001", StubPhoneVerificationProvider.mobile("+919876580001", "Airtel"));
        provider.script("+919876580002", StubPhoneVerificationProvider.invalid("+919876580002"));

        verificationService.start(new StartVerificationRequest(
                List.of(willVerify.getId(), willFail.getId()), null, null, null), ANALYST);
        drain();

        var page = verificationService.getVerifiedCompanies("Table ", null, null, null, null,
                0, 25, "verifiedAt", "desc");

        assertThat(page.content()).extracting("id").contains(willVerify.getId());
        assertThat(page.content()).extracting("id").doesNotContain(willFail.getId());

        var verifiedRow = page.content().stream()
                .filter(row -> row.id().equals(willVerify.getId())).findFirst().orElseThrow();
        assertThat(verifiedRow.lineType()).isEqualTo("mobile");
        assertThat(verifiedRow.carrierName()).isEqualTo("Airtel");
        assertThat(verifiedRow.provider()).isEqualTo("stub");
        assertThat(verifiedRow.verifiedPhone()).isEqualTo("+919876580001");
        assertThat(verifiedRow.verifiedByName()).isEqualTo("Priya Sharma");
        assertThat(verifiedRow.addedByName()).isEqualTo("Priya Sharma");
    }

    @Test
    void verifiedCompanies_filterByVerifiedByAndAddedBy() {
        Company company = company("Filter Verified Corp", "9876580010", ANALYST_2);
        provider.script("+919876580010", StubPhoneVerificationProvider.mobile("+919876580010", "Airtel"));

        verificationService.start(new StartVerificationRequest(
                List.of(company.getId()), null, null, null), SALES_LEAD);
        drain();

        assertThat(verificationService.getVerifiedCompanies(null, SALES_LEAD, null, null, ANALYST_2,
                0, 25, "verifiedAt", "desc").content())
                .extracting("id").contains(company.getId());

        // Wrong verifier, right adder: must not match.
        assertThat(verificationService.getVerifiedCompanies(null, ANALYST, null, null, ANALYST_2,
                0, 25, "verifiedAt", "desc").content())
                .extracting("id").doesNotContain(company.getId());
    }

    @Test
    void history_listsCompletedJobsWithTheirFilterSnapshot() {
        Company company = company("History Corp", "9876590001", ANALYST);
        provider.script("+919876590001", StubPhoneVerificationProvider.mobile("+919876590001", "Airtel"));
        LocalDate today = LocalDate.now();

        VerificationBatchResponse created = verificationService.start(
                new StartVerificationRequest(List.of(company.getId()), ANALYST, today, today), ANALYST);
        drain();

        var history = verificationService.list(null, ANALYST, null, null, 0, 25, "createdAt", "desc");
        var row = history.content().stream()
                .filter(b -> b.id().equals(created.id())).findFirst().orElseThrow();

        assertThat(row.status()).isEqualTo("COMPLETED");
        assertThat(row.requestedByName()).isEqualTo("Priya Sharma");
        assertThat(row.filterAddedBy()).isEqualTo(ANALYST);
        assertThat(row.filterAddedByName()).isEqualTo("Priya Sharma");
        assertThat(row.filterDateFrom()).isEqualTo(today);
        assertThat(row.filterDateTo()).isEqualTo(today);
        assertThat(row.totalCount()).isEqualTo(1);
        assertThat(row.verifiedCount()).isEqualTo(1);
        assertThat(row.completedAt()).isNotNull();
    }

    @Test
    void history_filtersByStatus() {
        Company company = company("Status Filter Corp", "9876590010", ANALYST);
        provider.script("+919876590010", StubPhoneVerificationProvider.invalid("+919876590010"));

        VerificationBatchResponse created = verificationService.start(
                new StartVerificationRequest(List.of(company.getId()), null, null, null), ANALYST);
        drain();

        assertThat(verificationService.list("COMPLETED_WITH_ERRORS", null, null, null,
                0, 50, "createdAt", "desc").content())
                .extracting("id").contains(created.id());
        assertThat(verificationService.list("CANCELLED", null, null, null,
                0, 50, "createdAt", "desc").content())
                .extracting("id").doesNotContain(created.id());
    }

    @Test
    void items_areInspectableWithStatusAndSearchFilters() {
        Company good = company("Item Good Corp", "9876600001", ANALYST);
        Company bad = company("Item Bad Corp", "9876600002", ANALYST);
        provider.script("+919876600001", StubPhoneVerificationProvider.mobile("+919876600001", "Airtel"));
        provider.script("+919876600002", StubPhoneVerificationProvider.invalid("+919876600002"));

        VerificationBatchResponse created = verificationService.start(
                new StartVerificationRequest(List.of(good.getId(), bad.getId()), null, null, null), ANALYST);
        drain();

        var all = verificationService.getItems(created.id(), null, null, 0, 25, "createdAt", "asc");
        assertThat(all.content()).hasSize(2);
        assertThat(all.content()).allMatch(item -> item.companyName() != null);

        var failedOnly = verificationService.getItems(created.id(), "FAILED", null,
                0, 25, "createdAt", "asc");
        assertThat(failedOnly.content()).hasSize(1);
        assertThat(failedOnly.content().getFirst().companyId()).isEqualTo(bad.getId());
        assertThat(failedOnly.content().getFirst().failureCode()).isEqualTo("INVALID_NUMBER");
        assertThat(failedOnly.content().getFirst().failureMessage()).isNotBlank();

        var searched = verificationService.getItems(created.id(), null, "Item Good",
                0, 25, "createdAt", "asc");
        assertThat(searched.content()).hasSize(1);
        assertThat(searched.content().getFirst().companyId()).isEqualTo(good.getId());
    }

    @Test
    void items_forAnUnknownBatch_is404() {
        assertThatThrownBy(() -> verificationService.getItems(UUID.randomUUID(), null, null,
                0, 25, "createdAt", "asc"))
                .isInstanceOf(com.vyoog.prospectsoul_backend.common.exception.ResourceNotFoundException.class);
    }

    @Test
    void getById_unknownBatch_is404() {
        assertThatThrownBy(() -> verificationService.getById(UUID.randomUUID()))
                .isInstanceOf(com.vyoog.prospectsoul_backend.common.exception.ResourceNotFoundException.class);
    }

    @Test
    void addedByOptions_resolveDisplayNamesForActorsThatActuallyAddedCompanies() {
        company("Option Corp", "9876610001", ANALYST);

        var options = verificationService.getAddedByOptions();

        assertThat(options).anyMatch(option ->
                ANALYST.equals(option.id())
                        && "Priya Sharma".equals(option.name())
                        && option.companyCount() >= 1);
    }

    @Test
    void pageSizeIsCapped() {
        var page = verificationService.getEligible(null, null, null, null, null,
                0, 5000, "createdAt", "desc");

        assertThat(page.size()).isEqualTo(100);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Runs worker passes until the queue is empty. Bounded so a bug that fails
     * to move an item to a terminal state fails the test instead of hanging.
     */
    private void drain() {
        for (int pass = 0; pass < 200; pass++) {
            if (itemRepository.countByStatus(VerificationItemStatus.QUEUED) == 0
                    && itemRepository.countByStatus(VerificationItemStatus.PROCESSING) == 0) {
                worker.reconcileNonTerminalBatches();
                return;
            }
            worker.runOnce();
        }
        throw new AssertionError("worker did not drain the queue within 200 passes");
    }

    private Company company(String name, String phone, String createdBy) {
        return companyRepository.save(Company.builder()
                .canonicalName(name)
                .normalizedName(name.toLowerCase().replace(" ", ""))
                .primaryPhoneNormalized(phone)
                .city("Coimbatore")
                .state("Tamil Nadu")
                .source("MANUAL_ENTRY")
                .createdBy(createdBy)
                .updatedBy(createdBy)
                .build());
    }

    /**
     * Creates a company with a back-dated {@code created_at}.
     *
     * <p>{@code Company.createdAt} is mapped {@code updatable = false} — a
     * record's lineage timestamp is not something the application may rewrite —
     * so the only way to place a fixture on a specific day is a direct SQL
     * update.
     */
    private Company companyCreatedAt(String name, String phone, String createdBy, Instant createdAt) {
        Company company = company(name, phone, createdBy);
        jdbcTemplate.update("UPDATE companies SET created_at = ? WHERE id = ?",
                java.sql.Timestamp.from(createdAt), company.getId());
        return companyRepository.findById(company.getId()).orElseThrow();
    }

    private Company reload(Company company) {
        return companyRepository.findById(company.getId()).orElseThrow();
    }

    private VerificationBatchItem itemFor(UUID batchId, UUID companyId) {
        return itemRepository.findByBatchIdAndCompanyId(batchId, companyId).orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, Object> contentOf(Activity activity) {
        return objectMapper.readValue(activity.getContent(), java.util.Map.class);
    }
}
