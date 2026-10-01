package com.vyoog.prospectsoul_backend.company.phone.dto.request;

import java.util.UUID;

import com.vyoog.prospectsoul_backend.company.phone.entity.ConfidenceLevel;
import com.vyoog.prospectsoul_backend.company.phone.entity.NumberSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CompanyPhoneRequest(
        UUID id,
        @NotBlank(message = "Phone number is required")
        String numberRaw,
        @NotNull(message = "Number source is required")
        NumberSourceType numberSource,
        ConfidenceLevel confidence,
        UUID contactId,
        String designationOverride,
        Boolean isPrimary
) {}
