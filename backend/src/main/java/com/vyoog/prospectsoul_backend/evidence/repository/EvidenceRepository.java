package com.vyoog.prospectsoul_backend.evidence.repository;

import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.evidence.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceRepository extends JpaRepository<Evidence, UUID> {

    List<Evidence> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    List<Evidence> findByEnrichmentJobIdOrderByCreatedAtDesc(UUID enrichmentJobId);

    List<Evidence> findByCompanyIdAndProviderKeyOrderByCreatedAtDesc(UUID companyId, String providerKey);
}
