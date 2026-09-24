package com.vyoog.prospectsoul_backend.evidence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "evidence")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "contact_id")
    private UUID contactId;

    @Column(name = "observation_type", nullable = false, length = 30)
    private String observationType;

    @Column(name = "provider_key", length = 40)
    private String providerKey;

    @Column(name = "enrichment_job_id")
    private UUID enrichmentJobId;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "excerpt")
    private String excerpt;

    @Column(name = "ai_model", length = 100)
    private String aiModel;

    @Column(name = "raw_payload_ref")
    private String rawPayloadRef;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload_inline", columnDefinition = "jsonb")
    private String rawPayloadInline;

    @Column(name = "observed_at")
    private Instant observedAt;

    @Column(name = "capture_date")
    private Instant captureDate;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
