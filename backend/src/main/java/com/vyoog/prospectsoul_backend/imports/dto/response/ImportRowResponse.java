package com.vyoog.prospectsoul_backend.imports.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ImportRowResponse(
        UUID id,
        Integer rowNumber,
        Object rawData,
        Object mappedData,
        String status,
        String errorMessage,
        UUID companyId,
        UUID duplicateOfCompanyId,
        Instant createdAt
) {}
