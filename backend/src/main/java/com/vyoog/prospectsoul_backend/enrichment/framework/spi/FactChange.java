package com.vyoog.prospectsoul_backend.enrichment.framework.spi;

public record FactChange(
    FactEntity entity,
    String field,
    String oldValue,
    String newValue,
    boolean overwritesHuman
) {
    public enum FactEntity { COMPANY, CONTACT }
}
