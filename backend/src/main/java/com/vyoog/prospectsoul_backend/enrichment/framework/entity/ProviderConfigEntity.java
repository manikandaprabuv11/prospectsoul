package com.vyoog.prospectsoul_backend.enrichment.framework.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "provider_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProviderConfigEntity {

    @Id
    @Column(name = "provider_key", length = 40)
    private String providerKey;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "rate_limit_per_sec")
    private Integer rateLimitPerSec;

    @Column(name = "rate_limit_per_day")
    private Integer rateLimitPerDay;

    @Column(name = "timeout_ms", nullable = false)
    @Builder.Default
    private Integer timeoutMs = 30000;

    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Short maxRetries = 3;

    @Column(name = "idempotency_window_hours", nullable = false)
    @Builder.Default
    private Short idempotencyWindowHours = 24;

    @Column(name = "cost_per_call_usd", nullable = false, precision = 10, scale = 6)
    @Builder.Default
    private BigDecimal costPerCallUsd = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String options;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @PrePersist
    @PreUpdate
    void prePersistUpdate() {
        updatedAt = Instant.now();
    }
}
