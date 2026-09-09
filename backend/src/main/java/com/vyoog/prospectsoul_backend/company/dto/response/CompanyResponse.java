package com.vyoog.prospectsoul_backend.company.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String canonicalName,
        String normalizedName,
        String websiteDomain,
        String primaryPhoneNormalized,
        String email,
        String city,
        String state,
        String cluster,
        String industry,
        String sizeBand,
        List<String> tags,
        String source,
        String pipelineState,
        Integer completenessScore,
        String verificationStatus,
        String verifiedBy,
        Instant verifiedAt,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt
) {}
