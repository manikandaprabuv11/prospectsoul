package com.vyoog.prospectsoul_backend.contactrole.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContactRoleCreateRequest(
        @NotBlank @Size(max = 40)
        @Pattern(regexp = "^[A-Z0-9_]{1,40}$", message = "key must be uppercase letters/digits/underscore")
        String key,

        @NotBlank @Size(max = 80) String label,

        Short sortOrder
) {}
