package com.vyoog.prospectsoul_backend.enrichment.framework.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vyoog.prospectsoul_backend.enrichment.framework.spi.EnrichmentProvider;
import org.springframework.stereotype.Service;

@Service
public class ProviderRegistry {

    private final Map<String, EnrichmentProvider> providers;

    public ProviderRegistry(List<EnrichmentProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(EnrichmentProvider::key, Function.identity()));
    }

    public Optional<EnrichmentProvider> get(String key) {
        return Optional.ofNullable(providers.get(key));
    }

    public List<String> allKeys() {
        return List.copyOf(providers.keySet());
    }
}
