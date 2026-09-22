package com.vyoog.prospectsoul_backend.contactrole.dto.request;

import jakarta.validation.constraints.Size;

public record ContactRoleUpdateRequest(
        @Size(max = 80) String label,
        Short sortOrder,
        Boolean active
) {}
