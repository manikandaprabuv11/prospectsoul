package com.vyoog.prospectsoul_backend.enrichment.framework.dto.response;

import java.time.Instant;
import java.util.UUID;

public record EnrichmentCandidateResponse(
        UUID id,
        UUID company_id,
        UUID enrichment_job_id,
        String candidate_type,
        String field_name,
        String proposed_value,
        String current_value,
        String status,
        String provider_key,
        UUID resolved_by,
        Instant resolved_at,
        Instant created_at
) {}
