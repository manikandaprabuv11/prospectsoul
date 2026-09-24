package com.vyoog.prospectsoul_backend.enrichment.framework.spi;

public record Candidate(
    String candidateType,
    String fieldName,
    String proposedValue,
    String currentValue
) {}
