package com.vyoog.prospectsoul_backend.nic.entity;

import java.time.Instant;
import java.util.UUID;

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

@Entity
@Table(name = "nic_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NicCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nic_data_id")
    private Integer nicDataId;

    @Column(nullable = false, length = 6, unique = true)
    private String code;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "industry_type", nullable = false, length = 20)
    private String industryType;

    @Column(nullable = false)
    private Short level;

    // Kept as a lazy association so callers can navigate the tree from an
    // entity, but the recursive descendant walk lives in the repository and
    // never triggers this fetch.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private NicCode parent;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

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

    public UUID getParentId() {
        return parent == null ? null : parent.getId();
    }
}
