package com.vyoog.prospectsoul_backend.company.nic.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CompanyNicCodeResponse(
        UUID id,
        UUID companyId,
        UUID nicCodeId,
        String nicCodeRaw,
        String resolvedCode,
        String resolvedDescription,
        String descriptionRaw,
        Boolean isPrimary,
        Short sequenceNo,
        Instant createdAt
) {}
