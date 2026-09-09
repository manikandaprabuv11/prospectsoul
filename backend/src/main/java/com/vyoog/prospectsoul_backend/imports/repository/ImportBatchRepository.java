package com.vyoog.prospectsoul_backend.imports.repository;

import java.util.UUID;

import com.vyoog.prospectsoul_backend.imports.entity.ImportBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, UUID> {

    Page<ImportBatch> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
