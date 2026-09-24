package com.vyoog.prospectsoul_backend.enrichment.framework.mapper;

import com.vyoog.prospectsoul_backend.enrichment.framework.dto.response.EnrichmentCandidateResponse;
import com.vyoog.prospectsoul_backend.enrichment.framework.dto.response.EnrichmentJobResponse;
import com.vyoog.prospectsoul_backend.enrichment.framework.dto.response.ProviderConfigResponse;
import com.vyoog.prospectsoul_backend.enrichment.framework.entity.EnrichmentCandidateEntity;
import com.vyoog.prospectsoul_backend.enrichment.framework.entity.EnrichmentJobEntity;
import com.vyoog.prospectsoul_backend.enrichment.framework.entity.ProviderConfigEntity;
import org.springframework.stereotype.Component;

@Component
public class EnrichmentMapper {

    public EnrichmentJobResponse toJobResponse(EnrichmentJobEntity entity) {
        return new EnrichmentJobResponse(
                entity.getId(),
                entity.getCompanyId(),
                entity.getProviderKey(),
                entity.getStatus(),
                entity.getAttempt(),
                entity.getMaxAttempts(),
                entity.getFactsAdded(),
                entity.getFactsUpdated(),
                entity.getCandidatesAdded(),
                entity.getCostUsd(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getTriggeredVia(),
                entity.getBatchId(),
                entity.getCreatedAt()
        );
    }

    public EnrichmentCandidateResponse toCandidateResponse(EnrichmentCandidateEntity entity) {
        return new EnrichmentCandidateResponse(
                entity.getId(),
                entity.getCompanyId(),
                entity.getEnrichmentJobId(),
                entity.getCandidateType(),
                entity.getFieldName(),
                entity.getProposedValue(),
                entity.getCurrentValue(),
                entity.getStatus(),
                entity.getProviderKey(),
                entity.getResolvedBy(),
                entity.getResolvedAt(),
                entity.getCreatedAt()
        );
    }

    public ProviderConfigResponse toConfigResponse(ProviderConfigEntity entity) {
        return new ProviderConfigResponse(
                entity.getProviderKey(),
                entity.getEnabled(),
                entity.getRateLimitPerSec(),
                entity.getRateLimitPerDay(),
                entity.getTimeoutMs(),
                entity.getMaxRetries(),
                entity.getIdempotencyWindowHours(),
                entity.getCostPerCallUsd(),
                entity.getUpdatedAt()
        );
    }
}
