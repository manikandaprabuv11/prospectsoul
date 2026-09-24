package com.vyoog.prospectsoul_backend.enrichment.framework.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "enrichment_candidates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EnrichmentCandidateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "contact_id")
    private UUID contactId;

    @Column(name = "enrichment_job_id", nullable = false)
    private UUID enrichmentJobId;

    @Column(name = "candidate_type", nullable = false, length = 30)
    private String candidateType;

    @Column(name = "field_name", length = 60)
    private String fieldName;

    @Column(name = "proposed_value")
    private String proposedValue;

    @Column(name = "current_value")
    private String currentValue;

    @Column(name = "provider_key", nullable = false, length = 40)
    private String providerKey;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
