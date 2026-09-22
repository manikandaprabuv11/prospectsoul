package com.vyoog.prospectsoul_backend.location.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pincode_centroids")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PincodeCentroid {

    @Id
    @Column(length = 6)
    private String pincode;

    @Column(name = "area_name", nullable = false, length = 255)
    private String areaName;

    @Column(length = 120)
    private String district;

    @Column(length = 120)
    private String state;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
