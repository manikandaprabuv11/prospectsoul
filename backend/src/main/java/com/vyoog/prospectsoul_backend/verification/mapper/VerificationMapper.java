package com.vyoog.prospectsoul_backend.verification.mapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.verification.dto.response.EligibleCompanyResponse;
import com.vyoog.prospectsoul_backend.verification.dto.response.VerificationBatchItemResponse;
import com.vyoog.prospectsoul_backend.verification.dto.response.VerificationBatchResponse;
import com.vyoog.prospectsoul_backend.verification.dto.response.VerifiedCompanyResponse;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationBatch;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationBatchItem;
import com.vyoog.prospectsoul_backend.verification.service.ActorNameResolver;
import org.springframework.stereotype.Component;

/**
 * Entity to response-DTO translation. No JPA entity ever leaves the service
 * layer (docs/dev_docs/14 §2).
 */
@Component
public class VerificationMapper {

    public VerificationBatchResponse toResponse(VerificationBatch batch,
                                                 Map<String, String> actorNames,
                                                 VerificationBatchResponse.CurrentItem currentItem) {
        return new VerificationBatchResponse(
                batch.getId(),
                batch.getStatus().name(),
                batch.getRequestedBy(),
                ActorNameResolver.nameOf(actorNames, batch.getRequestedBy()),
                batch.getFilterAddedBy(),
                ActorNameResolver.nameOf(actorNames, batch.getFilterAddedBy()),
                batch.getFilterDateFrom(),
                batch.getFilterDateTo(),
                batch.getTotalCount(),
                batch.getQueuedCount(),
                batch.getProcessingCount(),
                batch.getVerifiedCount(),
                batch.getFailedCount(),
                batch.getSkippedCount(),
                progressPercent(batch),
                elapsedSeconds(batch),
                currentItem,
                batch.getStartedAt(),
                batch.getCompletedAt(),
                batch.getCreatedAt(),
                batch.getUpdatedAt());
    }

    public VerificationBatchItemResponse toResponse(VerificationBatchItem item, String companyName) {
        return new VerificationBatchItemResponse(
                item.getId(),
                item.getBatchId(),
                item.getCompanyId(),
                companyName,
                item.getStatus().name(),
                item.getPhoneNumber(),
                item.getNormalizedPhoneNumber(),
                item.getProvider(),
                item.getProviderReference(),
                item.getPhoneValid(),
                item.getLineType(),
                item.getCarrierName(),
                item.getMobileCountryCode(),
                item.getMobileNetworkCode(),
                item.getFailureCode() == null ? null : item.getFailureCode().name(),
                item.getFailureMessage(),
                item.getAttemptCount(),
                item.getStartedAt(),
                item.getCompletedAt(),
                item.getCreatedAt());
    }

    public EligibleCompanyResponse toEligibleResponse(Company company,
                                                       Map<String, String> actorNames,
                                                       boolean phoneUsable,
                                                       boolean inFlight) {
        return new EligibleCompanyResponse(
                company.getId(),
                company.getCanonicalName(),
                company.getCity(),
                company.getState(),
                company.getPrimaryPhoneNormalized(),
                company.getVerificationStatus().name(),
                company.getCreatedBy(),
                ActorNameResolver.nameOf(actorNames, company.getCreatedBy()),
                company.getCreatedAt(),
                phoneUsable,
                inFlight);
    }

    public VerifiedCompanyResponse toVerifiedResponse(Company company,
                                                       Map<String, String> actorNames,
                                                       VerificationBatchItem latestItem) {
        return new VerifiedCompanyResponse(
                company.getId(),
                company.getCanonicalName(),
                company.getCity(),
                company.getState(),
                company.getVerifiedBy(),
                ActorNameResolver.nameOf(actorNames, company.getVerifiedBy()),
                company.getVerifiedAt(),
                company.getCreatedBy(),
                ActorNameResolver.nameOf(actorNames, company.getCreatedBy()),
                company.getCreatedAt(),
                latestItem != null ? latestItem.getNormalizedPhoneNumber() : company.getPrimaryPhoneNormalized(),
                latestItem != null ? latestItem.getLineType() : null,
                latestItem != null ? latestItem.getCarrierName() : null,
                latestItem != null ? latestItem.getProvider() : null);
    }

    /**
     * Terminal items over total. Skipped items count as done: they will never
     * be processed, so leaving them out would pin a batch at 98% forever.
     */
    static int progressPercent(VerificationBatch batch) {
        int total = batch.getTotalCount() == null ? 0 : batch.getTotalCount();
        if (total <= 0) {
            return 100;
        }
        int done = nz(batch.getVerifiedCount()) + nz(batch.getFailedCount()) + nz(batch.getSkippedCount());
        return (int) Math.min(100, Math.round((done * 100.0) / total));
    }

    /** Elapsed time to completion, or to now while the batch is still running. */
    static Long elapsedSeconds(VerificationBatch batch) {
        if (batch.getStartedAt() == null) {
            return null;
        }
        Instant end = batch.getCompletedAt() != null ? batch.getCompletedAt() : Instant.now();
        return Math.max(0, Duration.between(batch.getStartedAt(), end).toSeconds());
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
