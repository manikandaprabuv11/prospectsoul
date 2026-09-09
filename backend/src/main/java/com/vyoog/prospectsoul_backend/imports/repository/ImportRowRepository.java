package com.vyoog.prospectsoul_backend.imports.repository;

import java.util.UUID;

import com.vyoog.prospectsoul_backend.imports.entity.ImportRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImportRowRepository extends JpaRepository<ImportRow, UUID> {

    Page<ImportRow> findByBatchIdOrderByRowNumberAsc(UUID batchId, Pageable pageable);

    @Query("SELECT COUNT(r) FROM ImportRow r WHERE r.batch.id = :batchId AND r.status = :status")
    long countByBatchIdAndStatus(@Param("batchId") UUID batchId, @Param("status") ImportRow.RowStatus status);
}
