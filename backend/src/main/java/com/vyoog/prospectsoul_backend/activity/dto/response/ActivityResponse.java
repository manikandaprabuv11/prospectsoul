package com.vyoog.prospectsoul_backend.activity.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ActivityResponse(
        UUID id,
        UUID companyId,
        String type,
        String content,
        boolean verified,
        String createdBy,
        Instant createdAt
) {}
