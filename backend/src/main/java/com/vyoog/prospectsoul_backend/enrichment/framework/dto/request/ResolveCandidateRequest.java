package com.vyoog.prospectsoul_backend.enrichment.framework.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ResolveCandidateRequest(
        @NotNull(message = "Action is required")
        @Pattern(regexp = "ACCEPTED|REJECTED", message = "Action must be ACCEPTED or REJECTED")
        String action
) {}
