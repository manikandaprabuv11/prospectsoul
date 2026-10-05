package com.vyoog.prospectsoul_backend.company.phone.entity;

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

import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.contact.entity.Contact;

@Entity
@Table(name = "company_phones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyPhone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @Column(name = "number_raw", nullable = false)
    private String numberRaw;

    @Column(name = "number_normalized", length = 10)
    private String numberNormalized;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "phone_type", nullable = false)
    @Builder.Default
    private PhoneType phoneType = PhoneType.MOBILE;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "number_source", nullable = false)
    private NumberSourceType numberSource;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "confidence", nullable = false)
    @Builder.Default
    private ConfidenceLevel confidence = ConfidenceLevel.MEDIUM;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "confidence_mode", nullable = false)
    @Builder.Default
    private ConfidenceMode confidenceMode = ConfidenceMode.AUTO;

    @Column(name = "designation_override")
    private String designationOverride;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "override_reason")
    private String overrideReason;

    @Column(name = "enriched_country", length = 3)
    private String enrichedCountry;

    @Column(name = "enriched_region", length = 60)
    private String enrichedRegion;

    @Column(name = "enriched_carrier", length = 60)
    private String enrichedCarrier;

    @Column(name = "enriched_line_type", length = 20)
    private String enrichedLineType;

    @Column(name = "enriched_status", length = 20)
    private String enrichedStatus;

    @Column(name = "enriched_dnd")
    private Boolean enrichedDnd;

    @Column(name = "enriched_at")
    private Instant enrichedAt;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "import_row_id")
    private UUID importRowId;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

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

    public String resolveDesignation() {
        if (contact != null && contact.getDesignation() != null && !contact.getDesignation().isBlank()) {
            return contact.getDesignation();
        }
        return designationOverride;
    }
}
