package com.vyoog.prospectsoul_backend.imports.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "import_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "file_type", nullable = false, length = 20)
    private String fileType;

    @Column(nullable = false, length = 50)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private BatchStatus status = BatchStatus.UPLOADED;

    @Column(name = "total_rows", nullable = false)
    @Builder.Default
    private Integer totalRows = 0;

    @Column(name = "processed_rows", nullable = false)
    @Builder.Default
    private Integer processedRows = 0;

    @Column(name = "created_rows", nullable = false)
    @Builder.Default
    private Integer createdRows = 0;

    @Column(name = "duplicate_rows", nullable = false)
    @Builder.Default
    private Integer duplicateRows = 0;

    @Column(name = "rejected_rows", nullable = false)
    @Builder.Default
    private Integer rejectedRows = 0;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "column_mappings", columnDefinition = "jsonb")
    private String columnMappings;

    @Column(name = "created_by", length = 255)
    private String createdBy;

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

    public enum BatchStatus {
        UPLOADED, MAPPING, PREVIEWING, VALIDATED, PROCESSING, COMPLETED, FAILED
    }
}
