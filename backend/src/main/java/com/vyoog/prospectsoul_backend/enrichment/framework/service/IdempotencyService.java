package com.vyoog.prospectsoul_backend.enrichment.framework.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.enrichment.framework.entity.EnrichmentJobEntity;
import com.vyoog.prospectsoul_backend.enrichment.framework.repository.EnrichmentJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final EnrichmentJobRepository jobRepository;

    public String computeHash(String providerKey, UUID entityId, Map<String, Object> input) {
        var sorted = new TreeMap<>(input != null ? input : Map.of());
        String raw = providerKey + "|" + entityId + "|" + sorted;
        return sha256(raw);
    }

    public Optional<EnrichmentJobEntity> findCached(String providerKey, String inputHash,
                                                     UUID entityId, int windowHours) {
        Instant since = Instant.now().minus(windowHours, ChronoUnit.HOURS);
        return jobRepository.findIdempotent(providerKey, inputHash, entityId, since);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
