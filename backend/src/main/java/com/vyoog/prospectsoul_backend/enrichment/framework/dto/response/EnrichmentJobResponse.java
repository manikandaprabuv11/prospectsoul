package com.vyoog.prospectsoul_backend.enrichment.framework.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EnrichmentJobResponse(
        UUID id,
        UUID company_id,
        String provider_key,
        String status,
        short attempt,
        short max_attempts,
        short facts_added,
        short facts_updated,
        short candidates_added,
        BigDecimal cost_usd,
        String error_code,
        String error_message,
        Instant started_at,
        Instant completed_at,
        String triggered_via,
        UUID batch_id,
        Instant created_at
) {}
