package com.vyoog.prospectsoul_backend.company.defaultfilter.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "company_default_filters")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyDefaultFilter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "filter_key", nullable = false, length = 80, unique = true)
    private String filterKey;

    @Column(nullable = false, length = 120)
    private String label;

    /** Operator applied to the filter — matches CompanySpecification vocabulary. */
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String operator = "eq";

    /** JSON value — scalar, [min,max] pair, or array. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String value;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Short sortOrder = 100;

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
}
