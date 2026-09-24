package com.vyoog.prospectsoul_backend.enrichment.framework.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record ProviderConfigResponse(
        String provider_key,
        boolean enabled,
        Integer rate_limit_per_sec,
        Integer rate_limit_per_day,
        int timeout_ms,
        short max_retries,
        short idempotency_window_hours,
        BigDecimal cost_per_call_usd,
        Instant updated_at
) {}
