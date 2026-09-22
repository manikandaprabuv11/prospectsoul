package com.vyoog.prospectsoul_backend.company.nic.entity;

import java.time.Instant;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.nic.entity.NicCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "company_nic_codes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyNicCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nic_code_id")
    private NicCode nicCode;

    @Column(name = "nic_code_raw", nullable = false, length = 6)
    private String nicCodeRaw;

    @Column(name = "description_raw", columnDefinition = "text")
    private String descriptionRaw;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "sequence_no", nullable = false)
    private Short sequenceNo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getNicCodeId() {
        return nicCode == null ? null : nicCode.getId();
    }
}
