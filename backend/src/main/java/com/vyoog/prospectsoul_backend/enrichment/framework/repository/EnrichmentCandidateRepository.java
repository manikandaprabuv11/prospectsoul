package com.vyoog.prospectsoul_backend.enrichment.framework.repository;

import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.enrichment.framework.entity.EnrichmentCandidateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrichmentCandidateRepository extends JpaRepository<EnrichmentCandidateEntity, UUID> {

    List<EnrichmentCandidateEntity> findByCompanyIdAndStatusOrderByCreatedAtDesc(UUID companyId, String status);

    List<EnrichmentCandidateEntity> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    Page<EnrichmentCandidateEntity> findByCompanyIdAndStatus(UUID companyId, String status, Pageable pageable);

    Page<EnrichmentCandidateEntity> findByCompanyIdOrderByCreatedAtDesc(UUID companyId, Pageable pageable);

    long countByCompanyIdAndStatus(UUID companyId, String status);
}
