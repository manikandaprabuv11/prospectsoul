package com.vyoog.prospectsoul_backend.nic.dto.response;

import java.time.Instant;
import java.util.UUID;

public record NicCodeResponse(
        UUID id,
        Integer nicDataId,
        String code,
        String description,
        String industryType,
        Short level,
        UUID parentId,
        Boolean isPrimary,
        Boolean active,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt
) {}
