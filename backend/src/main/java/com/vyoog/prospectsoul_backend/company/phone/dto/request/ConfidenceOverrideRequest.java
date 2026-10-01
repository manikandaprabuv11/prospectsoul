package com.vyoog.prospectsoul_backend.company.phone.dto.request;

import com.vyoog.prospectsoul_backend.company.phone.entity.ConfidenceLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConfidenceOverrideRequest(
        @NotNull(message = "Confidence level is required")
        ConfidenceLevel confidence,
        @NotNull(message = "Override reason is required")
        @Size(min = 10, message = "Override reason must be at least 10 characters")
        String reason
) {}
