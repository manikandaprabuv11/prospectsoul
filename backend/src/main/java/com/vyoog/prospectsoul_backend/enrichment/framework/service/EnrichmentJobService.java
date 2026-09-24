package com.vyoog.prospectsoul_backend.enrichment.framework.service;

import java.util.UUID;

import com.vyoog.prospectsoul_backend.enrichment.framework.entity.EnrichmentJobEntity;
import com.vyoog.prospectsoul_backend.enrichment.framework.repository.EnrichmentJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrichmentJobService {

    private final EnrichmentJobRepository jobRepository;

    @Transactional(readOnly = true)
    public Page<EnrichmentJobEntity> listByCompany(UUID companyId, Pageable pageable) {
        return jobRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<EnrichmentJobEntity> listAll(Pageable pageable) {
        return jobRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public EnrichmentJobEntity getById(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Enrichment job not found: " + jobId));
    }
}
