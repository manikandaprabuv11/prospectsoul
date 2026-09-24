package com.vyoog.prospectsoul_backend.enrichment.framework.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateProviderConfigRequest(
        Boolean enabled,
        @Min(value = 0, message = "Max retries cannot be negative")
        @Max(value = 10, message = "Max retries cannot exceed 10")
        Integer max_retries,
        @Min(value = 1, message = "Idempotency window must be at least 1 hour")
        Integer idempotency_window_hours,
        Integer rate_limit_per_sec,
        Integer rate_limit_per_day,
        BigDecimal cost_per_call_usd
) {}
