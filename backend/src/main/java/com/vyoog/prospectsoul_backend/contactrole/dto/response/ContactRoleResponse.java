package com.vyoog.prospectsoul_backend.contactrole.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ContactRoleResponse(
        UUID id,
        String key,
        String label,
        Short sortOrder,
        Boolean active,
        long usageCount,
        Instant createdAt,
        Instant updatedAt
) {}
