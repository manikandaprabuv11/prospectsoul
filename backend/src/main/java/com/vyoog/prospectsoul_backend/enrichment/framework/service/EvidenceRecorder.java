package com.vyoog.prospectsoul_backend.enrichment.framework.service;

import java.time.Instant;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.evidence.entity.Evidence;
import com.vyoog.prospectsoul_backend.evidence.repository.EvidenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class EvidenceRecorder {

    private static final int INLINE_THRESHOLD = 100_000;

    private final EvidenceRepository evidenceRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public Evidence record(UUID companyId, UUID contactId, String providerKey,
                           UUID enrichmentJobId, Object rawPayload, String actor) {
        String payloadJson = serialize(rawPayload);
        boolean inline = payloadJson == null || payloadJson.length() <= INLINE_THRESHOLD;

        Evidence evidence = Evidence.builder()
                .companyId(companyId)
                .contactId(contactId)
                .observationType("PROVIDER_OBSERVATION")
                .providerKey(providerKey)
                .enrichmentJobId(enrichmentJobId)
                .rawPayloadInline(inline ? payloadJson : null)
                .rawPayloadRef(inline ? null : storeToObjectStorage(payloadJson))
                .observedAt(Instant.now())
                .createdBy(actor)
                .build();
        return evidenceRepository.save(evidence);
    }

    private String serialize(Object payload) {
        if (payload == null) return null;
        if (payload instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException e) {
            return "{\"error\": \"serialization_failed\"}";
        }
    }

    private String storeToObjectStorage(String payload) {
        // TODO: integrate with object-storage abstraction when payloads exceed inline threshold
        // For now, truncate and store inline as a pragmatic fallback
        return "inline_overflow_" + UUID.randomUUID();
    }
}
