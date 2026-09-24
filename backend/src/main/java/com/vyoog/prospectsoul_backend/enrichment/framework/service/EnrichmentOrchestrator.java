package com.vyoog.prospectsoul_backend.enrichment.framework.service;

import java.time.Instant;
import java.util.*;

import com.vyoog.prospectsoul_backend.activity.entity.ActivityType;
import com.vyoog.prospectsoul_backend.activity.service.ActivityService;
import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.enrichment.framework.entity.EnrichmentJobEntity;
import com.vyoog.prospectsoul_backend.enrichment.framework.entity.ProviderConfigEntity;
import com.vyoog.prospectsoul_backend.enrichment.framework.repository.EnrichmentJobRepository;
import com.vyoog.prospectsoul_backend.enrichment.framework.repository.ProviderConfigRepository;
import com.vyoog.prospectsoul_backend.enrichment.framework.spi.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrichmentOrchestrator {

    private final ProviderRegistry providerRegistry;
    private final IdempotencyService idempotencyService;
    private final EvidenceRecorder evidenceRecorder;
    private final FactUpdater factUpdater;
    private final EnrichmentJobRepository jobRepository;
    private final ProviderConfigRepository configRepository;
    private final ActivityService activityService;
    private final AuditService auditService;
    private final TransactionTemplate txTemplate;

    public EnrichmentJobEntity enrich(UUID companyId, String providerKey,
                                       Map<String, Object> input, boolean force,
                                       UUID triggeredBy, String triggeredVia) {
        EnrichmentProvider provider = providerRegistry.get(providerKey)
                .orElseThrow(() -> new IllegalArgumentException("Unknown provider: " + providerKey));

        ProviderConfigEntity config = configRepository.findById(providerKey)
                .orElseThrow(() -> new IllegalArgumentException("No config for provider: " + providerKey));

        if (!config.getEnabled()) {
            throw new IllegalStateException("Provider " + providerKey + " is disabled");
        }

        String inputHash = idempotencyService.computeHash(providerKey, companyId, input);

        if (!force) {
            Optional<EnrichmentJobEntity> cached = idempotencyService.findCached(
                    providerKey, inputHash, companyId, config.getIdempotencyWindowHours());
            if (cached.isPresent()) {
                log.info("Idempotency hit for {} on company {}", providerKey, companyId);
                return cached.get();
            }
        }

        // Phase 1: create job row inside a transaction
        EnrichmentJobEntity job = txTemplate.execute(status -> {
            jobRepository.cancelStaleJobs(providerKey, inputHash, companyId);

            EnrichmentJobEntity j = jobRepository.save(EnrichmentJobEntity.builder()
                    .companyId(companyId)
                    .providerKey(providerKey)
                    .inputHash(inputHash)
                    .status("RUNNING")
                    .maxAttempts(config.getMaxRetries())
                    .startedAt(Instant.now())
                    .triggeredBy(triggeredBy)
                    .triggeredVia(triggeredVia)
                    .build());

            auditService.record("ENRICHMENT_JOB", j.getId(),
                    triggeredBy != null ? triggeredBy.toString() : "SYSTEM",
                    "CREATED", null, Map.of("provider", providerKey, "company_id", companyId));
            return j;
        });

        // Phase 2: call provider OUTSIDE any transaction (may be slow/external)
        ProviderResult result;
        try {
            ProviderRequest request = new ProviderRequest(companyId, null, input, inputHash);
            result = provider.execute(request);
        } catch (Exception e) {
            log.error("Enrichment failed for provider {} on company {}", providerKey, companyId, e);
            String errMsg = e.getMessage() != null
                    ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500))
                    : "Unknown error";
            return recordFailure(job, "EXCEPTION", errMsg, companyId, providerKey, triggeredBy);
        }

        if (result.status() == ProviderStatus.FAILED_TRANSIENT) {
            return handleTransientFailure(job, result, config);
        }
        if (result.status() == ProviderStatus.FAILED_PERMANENT) {
            return recordFailure(job, result.errorCode(), result.errorMessage(),
                    companyId, providerKey, triggeredBy);
        }

        // Phase 3: persist results inside a transaction
        return recordSuccess(job, result, companyId, providerKey, config, triggeredBy);
    }

    public List<EnrichmentJobEntity> enrichMultipleProviders(UUID companyId, List<String> providerKeys,
                                                              boolean force, UUID triggeredBy, String triggeredVia) {
        UUID batchId = UUID.randomUUID();
        List<EnrichmentJobEntity> jobs = new ArrayList<>();
        for (String key : providerKeys) {
            try {
                EnrichmentJobEntity job = enrich(companyId, key, Map.of(), force, triggeredBy, triggeredVia);
                job.setBatchId(batchId);
                jobRepository.save(job);
                jobs.add(job);
            } catch (Exception e) {
                log.error("Failed to enrich with provider {} for company {}", key, companyId, e);
            }
        }
        return jobs;
    }

    private EnrichmentJobEntity recordSuccess(EnrichmentJobEntity job, ProviderResult result,
                                               UUID companyId, String providerKey,
                                               ProviderConfigEntity config, UUID triggeredBy) {
        return txTemplate.execute(status -> {
            String actor = triggeredBy != null ? triggeredBy.toString() : "SYSTEM";

            evidenceRecorder.record(companyId, null, providerKey, job.getId(),
                    result.rawPayload(), actor);

            FactUpdater.FactUpdateResult factResult = factUpdater.applyFacts(
                    companyId, job.getId(), providerKey, result.facts(), result.candidates());

            job.setStatus(result.status() == ProviderStatus.PARTIAL ? "PARTIAL" : "SUCCESS");
            job.setCompletedAt(Instant.now());
            job.setFactsAdded((short) factResult.factsAdded());
            job.setFactsUpdated((short) factResult.factsUpdated());
            job.setCandidatesAdded((short) factResult.candidatesAdded());
            job.setCostUsd(result.costUsd() != null ? result.costUsd() : config.getCostPerCallUsd());
            jobRepository.save(job);

            activityService.record(companyId, ActivityType.ENRICHMENT,
                    Map.of("provider", providerKey,
                            "job_id", job.getId().toString(),
                            "facts_added", factResult.factsAdded(),
                            "facts_updated", factResult.factsUpdated(),
                            "candidates_added", factResult.candidatesAdded()),
                    false, actor);

            return job;
        });
    }

    private EnrichmentJobEntity recordFailure(EnrichmentJobEntity job, String errorCode, String errorMessage,
                                               UUID companyId, String providerKey, UUID triggeredBy) {
        return txTemplate.execute(status -> {
            String actor = triggeredBy != null ? triggeredBy.toString() : "SYSTEM";
            job.setStatus("FAILED");
            job.setCompletedAt(Instant.now());
            job.setErrorCode(errorCode);
            job.setErrorMessage(errorMessage);
            jobRepository.save(job);

            activityService.record(companyId, ActivityType.ENRICHMENT,
                    Map.of("provider", providerKey,
                            "job_id", job.getId().toString(),
                            "status", "FAILED",
                            "error", errorMessage != null ? errorMessage : "Unknown"),
                    false, actor);

            return job;
        });
    }

    private EnrichmentJobEntity handleTransientFailure(EnrichmentJobEntity job, ProviderResult result,
                                                        ProviderConfigEntity config) {
        return txTemplate.execute(status -> {
            if (job.getAttempt() < config.getMaxRetries()) {
                job.setStatus("QUEUED");
                job.setAttempt((short) (job.getAttempt() + 1));
                job.setScheduledAt(Instant.now().plusSeconds((long) Math.pow(4, job.getAttempt() - 1)));
                job.setErrorCode(result.errorCode());
                job.setErrorMessage(result.errorMessage());
            } else {
                job.setStatus("FAILED");
                job.setCompletedAt(Instant.now());
                job.setErrorCode(result.errorCode());
                job.setErrorMessage(result.errorMessage());
            }
            return jobRepository.save(job);
        });
    }
}
