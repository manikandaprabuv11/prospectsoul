package com.vyoog.prospectsoul_backend.verification.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.verification.entity.VerificationBatch;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface VerificationBatchRepository extends JpaRepository<VerificationBatch, UUID>,
        JpaSpecificationExecutor<VerificationBatch> {

    List<VerificationBatch> findByStatusIn(List<VerificationBatchStatus> statuses);

    /**
     * The single batch the Verify page's "Current Verification" card renders.
     * Newest non-terminal batch wins; the DB is the source of truth, so this
     * survives navigation, refresh and backend restart.
     */
    @Query("""
            SELECT b FROM VerificationBatch b
            WHERE b.status IN (com.vyoog.prospectsoul_backend.verification.entity.VerificationBatchStatus.QUEUED,
                               com.vyoog.prospectsoul_backend.verification.entity.VerificationBatchStatus.PROCESSING)
            ORDER BY b.createdAt DESC
            LIMIT 1
            """)
    Optional<VerificationBatch> findActive();

}
