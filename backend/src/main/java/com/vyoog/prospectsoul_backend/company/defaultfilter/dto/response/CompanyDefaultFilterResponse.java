package com.vyoog.prospectsoul_backend.company.defaultfilter.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CompanyDefaultFilterResponse(
        UUID id,
        String filterKey,
        String label,
        String operator,
        String value,
        Boolean active,
        Short sortOrder,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt
) {}
