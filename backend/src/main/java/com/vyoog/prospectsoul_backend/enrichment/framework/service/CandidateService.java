package com.vyoog.prospectsoul_backend.enrichment.framework.service;

import java.time.Instant;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.enrichment.framework.entity.EnrichmentCandidateEntity;
import com.vyoog.prospectsoul_backend.enrichment.framework.repository.EnrichmentCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CandidateService {

    private final EnrichmentCandidateRepository candidateRepository;
    private final FactUpdater factUpdater;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<EnrichmentCandidateEntity> listByCompany(UUID companyId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return candidateRepository.findByCompanyIdAndStatus(companyId, status, pageable);
        }
        return candidateRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, pageable);
    }

    @Transactional
    public EnrichmentCandidateEntity resolve(UUID candidateId, String action, String actor) {
        EnrichmentCandidateEntity candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found: " + candidateId));

        if (!"PENDING".equals(candidate.getStatus())) {
            throw new IllegalStateException("Candidate already resolved: " + candidate.getStatus());
        }

        if ("ACCEPTED".equals(action)) {
            factUpdater.setCompanyFieldPublic(candidate.getCompanyId(),
                    candidate.getFieldName(), candidate.getProposedValue());
            candidate.setStatus("ACCEPTED");
        } else {
            candidate.setStatus("REJECTED");
        }

        UUID actorUuid = UUID.fromString(actor);
        candidate.setResolvedBy(actorUuid);
        candidate.setResolvedAt(Instant.now());
        candidateRepository.save(candidate);

        auditService.record("ENRICHMENT_CANDIDATE", candidate.getId(), actor,
                action, candidate.getCurrentValue(), candidate.getProposedValue());

        return candidate;
    }
}
