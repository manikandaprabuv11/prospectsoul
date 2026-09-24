package com.vyoog.prospectsoul_backend.enrichment.framework.spi;

import java.math.BigDecimal;
import java.util.List;

public record ProviderResult(
    ProviderStatus status,
    List<FactChange> facts,
    List<Candidate> candidates,
    Object rawPayload,
    BigDecimal costUsd,
    String errorCode,
    String errorMessage
) {}
