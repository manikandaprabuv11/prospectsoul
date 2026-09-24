package com.vyoog.prospectsoul_backend.enrichment.framework.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.enrichment.framework.dto.request.UpdateProviderConfigRequest;
import com.vyoog.prospectsoul_backend.enrichment.framework.entity.ProviderConfigEntity;
import com.vyoog.prospectsoul_backend.enrichment.framework.repository.ProviderConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProviderConfigService {

    private final ProviderConfigRepository configRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ProviderConfigEntity> listAll() {
        return configRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ProviderConfigEntity getByKey(String providerKey) {
        return configRepository.findById(providerKey)
                .orElseThrow(() -> new IllegalArgumentException("Provider config not found: " + providerKey));
    }

    @Transactional
    public ProviderConfigEntity update(String providerKey, UpdateProviderConfigRequest request, String actor) {
        ProviderConfigEntity config = getByKey(providerKey);
        Map<String, Object> previousState = Map.of(
                "enabled", config.getEnabled(),
                "max_retries", config.getMaxRetries(),
                "idempotency_window_hours", config.getIdempotencyWindowHours(),
                "cost_per_call_usd", config.getCostPerCallUsd()
        );

        if (request.enabled() != null) config.setEnabled(request.enabled());
        if (request.max_retries() != null) config.setMaxRetries(request.max_retries().shortValue());
        if (request.idempotency_window_hours() != null) config.setIdempotencyWindowHours(request.idempotency_window_hours().shortValue());
        if (request.rate_limit_per_sec() != null) config.setRateLimitPerSec(request.rate_limit_per_sec());
        if (request.rate_limit_per_day() != null) config.setRateLimitPerDay(request.rate_limit_per_day());
        if (request.cost_per_call_usd() != null) config.setCostPerCallUsd(request.cost_per_call_usd());

        configRepository.save(config);

        auditService.record("PROVIDER_CONFIG",
                UUID.nameUUIDFromBytes(providerKey.getBytes(StandardCharsets.UTF_8)),
                actor, "UPDATED", previousState, request);

        return config;
    }
}
