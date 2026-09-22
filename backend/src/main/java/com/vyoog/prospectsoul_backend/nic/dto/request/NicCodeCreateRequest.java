package com.vyoog.prospectsoul_backend.nic.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NicCodeCreateRequest(
        @NotBlank
        @Pattern(regexp = "^[0-9]{1,5}$", message = "code must be 1-5 digits")
        String code,

        @NotBlank
        @Size(max = 4000)
        String description,

        @NotNull
        @Pattern(regexp = "^(Service|Manufacturing|Unknown)$",
                 message = "industry_type must be Service, Manufacturing or Unknown")
        String industryType,

        UUID parentId,

        Boolean isPrimary
) {}
