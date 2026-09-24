package com.vyoog.prospectsoul_backend.enrichment.framework.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.enrichment.framework.entity.EnrichmentJobEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EnrichmentJobRepository extends JpaRepository<EnrichmentJobEntity, UUID>,
        JpaSpecificationExecutor<EnrichmentJobEntity> {

    @Query("""
        SELECT j FROM EnrichmentJobEntity j
        WHERE j.providerKey = :providerKey
          AND j.inputHash = :inputHash
          AND COALESCE(j.companyId, j.contactId) = :entityId
          AND j.status IN ('QUEUED', 'RUNNING', 'SUCCESS')
          AND j.createdAt > :since
        ORDER BY j.createdAt DESC
        LIMIT 1
        """)
    Optional<EnrichmentJobEntity> findIdempotent(String providerKey, String inputHash,
                                                  UUID entityId, Instant since);

    List<EnrichmentJobEntity> findByBatchIdOrderByCreatedAtAsc(UUID batchId);

    Page<EnrichmentJobEntity> findByCompanyIdOrderByCreatedAtDesc(UUID companyId, Pageable pageable);

    @Query("""
        SELECT j FROM EnrichmentJobEntity j
        WHERE j.status = 'QUEUED'
          AND j.providerKey = :providerKey
          AND (j.scheduledAt IS NULL OR j.scheduledAt <= :now)
        ORDER BY j.createdAt ASC
        LIMIT :limit
        """)
    List<EnrichmentJobEntity> findQueuedJobs(String providerKey, Instant now, int limit);

    long countByBatchId(UUID batchId);

    long countByBatchIdAndStatus(UUID batchId, String status);

    @Query("SELECT COALESCE(SUM(j.costUsd), 0) FROM EnrichmentJobEntity j WHERE j.batchId = :batchId")
    java.math.BigDecimal totalCostByBatchId(UUID batchId);

    @Modifying
    @Query("""
        UPDATE EnrichmentJobEntity j SET j.status = 'CANCELLED', j.completedAt = CURRENT_TIMESTAMP,
        j.errorMessage = 'Superseded by force re-run'
        WHERE j.providerKey = :providerKey
          AND j.inputHash = :inputHash
          AND COALESCE(j.companyId, j.contactId) = :entityId
          AND j.status IN ('QUEUED', 'RUNNING', 'SUCCESS')
        """)
    int cancelStaleJobs(String providerKey, String inputHash, UUID entityId);
}
