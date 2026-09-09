package com.vyoog.prospectsoul_backend.common.audit.repository;

import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);
}
