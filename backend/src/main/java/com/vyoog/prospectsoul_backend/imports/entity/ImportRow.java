package com.vyoog.prospectsoul_backend.imports.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "import_rows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportRow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private ImportBatch batch;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", nullable = false, columnDefinition = "jsonb")
    private String rawData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mapped_data", columnDefinition = "jsonb")
    private String mappedData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private RowStatus status = RowStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "duplicate_of_company_id")
    private UUID duplicateOfCompanyId;

    @Column(name = "outcome_reason", length = 100)
    private String outcomeReason;

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

    public enum RowStatus {
        PENDING, CREATED, DUPLICATE, REJECTED, FAILED
    }
}
