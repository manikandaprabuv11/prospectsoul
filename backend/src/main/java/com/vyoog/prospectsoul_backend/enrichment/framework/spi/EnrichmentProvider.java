package com.vyoog.prospectsoul_backend.enrichment.framework.spi;

import java.util.Set;

public interface EnrichmentProvider {
    String key();
    Set<ProviderCapability> capabilities();
    ProviderResult execute(ProviderRequest request);
}
