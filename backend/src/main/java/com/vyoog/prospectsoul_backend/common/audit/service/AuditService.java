package com.vyoog.prospectsoul_backend.common.audit.service;

import java.util.List;
import java.util.UUID;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.vyoog.prospectsoul_backend.common.audit.entity.AuditLog;
import com.vyoog.prospectsoul_backend.common.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String entityType, UUID entityId, String actor, String action,
                       Object previousState, Object newState) {
        AuditLog log = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .actor(actor)
                .action(action)
                .previousState(toJson(previousState))
                .newState(toJson(newState))
                .build();
        auditLogRepository.save(log);
    }

    public List<AuditLog> getAuditHistory(String entityType, UUID entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JacksonException e) {
            return "{\"error\": \"serialization_failed\"}";
        }
    }
}
