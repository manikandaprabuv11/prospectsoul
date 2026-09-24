package com.vyoog.prospectsoul_backend.contact.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.contactrole.entity.ContactRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Contacts (ADR-0006). Multi-per-company. {@code role_id} is required on
 * new rows; {@code is_md_owner} is kept in sync in the service layer for
 * backward-compatible readers (ADR-0001).
 */
@Entity
@Table(name = "contacts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 255)
    private String designation;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "is_md_owner", nullable = false)
    @Builder.Default
    private Boolean isMdOwner = false;

    @Column(name = "association_start")
    private LocalDate associationStart;

    @Column(name = "association_end")
    private LocalDate associationEnd;

    @Column(name = "verification_status", nullable = false, length = 50)
    @Builder.Default
    private String verificationStatus = "UNVERIFIED";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private ContactRole role;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "phone_normalized", length = 20)
    private String phoneNormalized;

    @Column(name = "phone_country", length = 3)
    private String phoneCountry;

    @Column(name = "phone_region", length = 60)
    private String phoneRegion;

    @Column(name = "phone_carrier", length = 60)
    private String phoneCarrier;

    @Column(name = "phone_type", length = 20)
    private String phoneType;

    @Column(name = "phone_status", length = 20)
    private String phoneStatus;

    @Column(name = "phone_dnd_registered")
    private Boolean phoneDndRegistered;

    @Column(name = "phone_last_enriched_at")
    private Instant phoneLastEnrichedAt;

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

    public UUID getRoleId() {
        return role == null ? null : role.getId();
    }
}
