package com.vyoog.prospectsoul_backend.enrichment.framework.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.vyoog.prospectsoul_backend.enrichment.framework.spi.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class MockEnrichmentProvider implements EnrichmentProvider {

    public static final String KEY = "mock";

    private ProviderResult nextResult;

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public Set<ProviderCapability> capabilities() {
        return Set.of(ProviderCapability.COMPANY_ENRICHMENT);
    }

    @Override
    public ProviderResult execute(ProviderRequest request) {
        if (nextResult != null) {
            ProviderResult r = nextResult;
            nextResult = null;
            return r;
        }
        return new ProviderResult(
                ProviderStatus.SUCCESS,
                List.of(new FactChange(FactChange.FactEntity.COMPANY, "website_title",
                        null, "Mock Title", false)),
                List.of(),
                "{\"mock\": true}",
                new BigDecimal("0.001"),
                null,
                null
        );
    }

    public void setNextResult(ProviderResult result) {
        this.nextResult = result;
    }
}
