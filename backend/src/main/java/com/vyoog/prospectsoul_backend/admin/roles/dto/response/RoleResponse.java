package com.vyoog.prospectsoul_backend.admin.roles.dto.response;

import java.util.List;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        String name,
        String displayName,
        String description,
        List<String> permissions,
        long userCount,
        boolean active
) {}
