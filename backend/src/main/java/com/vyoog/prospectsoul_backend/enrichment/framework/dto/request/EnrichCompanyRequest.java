package com.vyoog.prospectsoul_backend.enrichment.framework.dto.request;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record EnrichCompanyRequest(
        @NotEmpty(message = "At least one provider key is required")
        List<String> provider_keys,
        Map<String, Object> input,
        boolean force
) {}
