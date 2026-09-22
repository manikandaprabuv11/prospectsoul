package com.vyoog.prospectsoul_backend.company.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "canonical_name", nullable = false, length = 500)
    private String canonicalName;

    @Column(name = "normalized_name", nullable = false, length = 500)
    private String normalizedName;

    @Column(name = "website_domain", length = 500)
    private String websiteDomain;

    @Column(name = "primary_phone_normalized", length = 20)
    private String primaryPhoneNormalized;

    @Column(length = 500)
    private String email;

    @Column(length = 200)
    private String city;

    @Column(length = 200)
    private String state;

    @Column(length = 200)
    private String cluster;

    @Column(length = 200)
    private String industry;

    @Column(name = "size_band", length = 50)
    private String sizeBand;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> tags;

    @Column(length = 50)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "pipeline_state", nullable = false, length = 50)
    @Builder.Default
    private PipelineState pipelineState = PipelineState.IMPORTED;

    @Column(name = "completeness_score", nullable = false)
    @Builder.Default
    private Integer completenessScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 50)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    @Column(name = "verified_by", length = 255)
    private String verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;


    @Column(length = 6)
    private String pincode;

    @Column(length = 120)
    private String district;

    @Column(name = "address_line", columnDefinition = "text")
    private String addressLine;

    @Column(length = 120)
    private String region;

    @Column(columnDefinition = "text")
    private String products;

    @Column(precision = 18, scale = 2)
    private BigDecimal turnover;

    @Column(name = "gst_number", length = 15)
    private String gstNumber;

    @Column(name = "employee_count")
    private Integer employeeCount;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Column(name = "source_reference", length = 120)
    private String sourceReference;

    @Column(name = "lg_state_code")
    private Short lgStateCode;

    @Column(name = "lg_district_code")
    private Integer lgDistrictCode;

    @Column(name = "primary_nic_code_id")
    private UUID primaryNicCodeId;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

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

    public enum PipelineState {
        IMPORTED, TRIAGE, RESEARCH, QUALIFICATION, READY, EXPORTED, DISQUALIFIED, ARCHIVED
    }

    public enum VerificationStatus {
        UNVERIFIED, VERIFIED, INVALIDATED
    }
}
