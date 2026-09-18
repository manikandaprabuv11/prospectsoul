package com.vyoog.prospectsoul_backend.verification.entity;

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

/**
 * One company inside a batch, and simultaneously one row of the DB-backed work
 * queue the worker claims with {@code FOR UPDATE SKIP LOCKED}.
 */
@Entity
@Table(name = "verification_batch_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationBatchItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private VerificationItemStatus status = VerificationItemStatus.QUEUED;

    /** The company phone as stored when the item was created. */
    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    /** E.164 form actually sent to the provider. */
    @Column(name = "normalized_phone_number", length = 50)
    private String normalizedPhoneNumber;

    @Column(length = 50)
    private String provider;

    @Column(name = "provider_reference", length = 500)
    private String providerReference;

    @Column(name = "phone_valid")
    private Boolean phoneValid;

    @Column(name = "line_type", length = 50)
    private String lineType;

    @Column(name = "carrier_name", length = 255)
    private String carrierName;

    @Column(name = "mobile_country_code", length = 10)
    private String mobileCountryCode;

    @Column(name = "mobile_network_code", length = 10)
    private String mobileNetworkCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_code", length = 50)
    private VerificationFailureCode failureCode;

    @Column(name = "failure_message", columnDefinition = "text")
    private String failureMessage;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

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
