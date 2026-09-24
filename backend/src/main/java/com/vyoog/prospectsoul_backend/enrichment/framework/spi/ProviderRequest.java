package com.vyoog.prospectsoul_backend.enrichment.framework.spi;

import java.util.Map;
import java.util.UUID;

public record ProviderRequest(
    UUID companyId,
    UUID contactId,
    Map<String, Object> input,
    String idempotencyKey
) {}
