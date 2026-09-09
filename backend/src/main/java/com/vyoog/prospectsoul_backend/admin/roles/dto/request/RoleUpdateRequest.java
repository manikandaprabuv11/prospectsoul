package com.vyoog.prospectsoul_backend.admin.roles.dto.request;

import jakarta.validation.constraints.Size;

public record RoleUpdateRequest(
        @Size(max = 100) String displayName,
        String description
) {}
