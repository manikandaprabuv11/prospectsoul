package com.vyoog.prospectsoul_backend.verification.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.verification.entity.VerificationBatchItem;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerificationBatchItemRepository extends JpaRepository<VerificationBatchItem, UUID>,
        JpaSpecificationExecutor<VerificationBatchItem> {

    /**
     * Claims the next queued item ids for this worker pass.
     *
     * <p>{@code FOR UPDATE SKIP LOCKED} is what makes the queue safe with more
     * than one worker or application instance: a row another transaction has
     * already locked is skipped instead of blocking, so two workers never
     * process the same company and no pass stalls behind another.
     */
    @Query(value = """
            SELECT id FROM verification_batch_items
            WHERE status = 'QUEUED'
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<UUID> claimQueuedItemIds(@Param("limit") int limit);

    @Modifying
    @Query(value = """
            UPDATE verification_batch_items
            SET status = 'PROCESSING',
                started_at = COALESCE(started_at, now()),
                attempt_count = attempt_count + 1,
                updated_at = now()
            WHERE id IN (:ids)
            """, nativeQuery = true)
    int markProcessing(@Param("ids") List<UUID> ids);

    /**
     * Restart recovery: an item left PROCESSING by a killed JVM has no worker
     * behind it any more, so it is returned to the queue once it is older than
     * the configured stale timeout.
     */
    @Modifying
    @Query(value = """
            UPDATE verification_batch_items
            SET status = 'QUEUED',
                updated_at = now()
            WHERE status = 'PROCESSING'
              AND started_at < :cutoff
            """, nativeQuery = true)
    int requeueStaleProcessingItems(@Param("cutoff") Instant cutoff);

    @Query("SELECT i.status, COUNT(i) FROM VerificationBatchItem i WHERE i.batchId = :batchId GROUP BY i.status")
    List<Object[]> countByStatusForBatch(@Param("batchId") UUID batchId);

    List<VerificationBatchItem> findByBatchId(UUID batchId);

    /** Companies with a non-terminal item — i.e. already being verified. */
    @Query("""
            SELECT DISTINCT i.companyId FROM VerificationBatchItem i
            WHERE i.companyId IN :companyIds
              AND i.status IN (com.vyoog.prospectsoul_backend.verification.entity.VerificationItemStatus.QUEUED,
                               com.vyoog.prospectsoul_backend.verification.entity.VerificationItemStatus.PROCESSING)
            """)
    List<UUID> findCompanyIdsInFlight(@Param("companyIds") List<UUID> companyIds);

    @Query("""
            SELECT DISTINCT i.companyId FROM VerificationBatchItem i
            WHERE i.status IN (com.vyoog.prospectsoul_backend.verification.entity.VerificationItemStatus.QUEUED,
                               com.vyoog.prospectsoul_backend.verification.entity.VerificationItemStatus.PROCESSING)
            """)
    List<UUID> findAllCompanyIdsInFlight();


    Optional<VerificationBatchItem> findByBatchIdAndCompanyId(UUID batchId, UUID companyId);

    /** The item the progress card's "Currently verifying" line describes. */
    Optional<VerificationBatchItem> findFirstByBatchIdAndStatusOrderByStartedAtDesc(
            UUID batchId, VerificationItemStatus status);

    /**
     * Provider details for the verified-companies table: the successful items
     * for a page of companies, newest first, so the caller can keep the first
     * one it sees per company.
     */
    @Query("""
            SELECT i FROM VerificationBatchItem i
            WHERE i.companyId IN :companyIds
              AND i.status = com.vyoog.prospectsoul_backend.verification.entity.VerificationItemStatus.VERIFIED
            ORDER BY i.completedAt DESC
            """)
    List<VerificationBatchItem> findLatestVerifiedForCompanies(@Param("companyIds") List<UUID> companyIds);

    long countByStatus(VerificationItemStatus status);
}
