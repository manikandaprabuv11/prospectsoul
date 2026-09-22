package com.vyoog.prospectsoul_backend.nic.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NicCodeUpdateRequest(
        @Size(max = 4000)
        String description,

        @Pattern(regexp = "^(Service|Manufacturing|Unknown)$")
        String industryType,

        UUID parentId,

        Boolean isPrimary,

        Boolean active,

        Boolean forceDeactivate
) {}
