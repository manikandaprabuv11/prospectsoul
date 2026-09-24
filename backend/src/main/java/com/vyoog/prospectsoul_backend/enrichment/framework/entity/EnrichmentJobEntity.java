package com.vyoog.prospectsoul_backend.enrichment.framework.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "enrichment_jobs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EnrichmentJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "contact_id")
    private UUID contactId;

    @Column(name = "provider_key", nullable = false, length = 40)
    private String providerKey;

    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    @Builder.Default
    private Short attempt = 1;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Short maxAttempts = 3;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "facts_added", nullable = false)
    @Builder.Default
    private Short factsAdded = 0;

    @Column(name = "facts_updated", nullable = false)
    @Builder.Default
    private Short factsUpdated = 0;

    @Column(name = "candidates_added", nullable = false)
    @Builder.Default
    private Short candidatesAdded = 0;

    @Column(name = "cost_usd", nullable = false, precision = 10, scale = 6)
    @Builder.Default
    private BigDecimal costUsd = BigDecimal.ZERO;

    @Column(name = "error_code", length = 60)
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "triggered_by")
    private UUID triggeredBy;

    @Column(name = "triggered_via", nullable = false, length = 20)
    private String triggeredVia;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
