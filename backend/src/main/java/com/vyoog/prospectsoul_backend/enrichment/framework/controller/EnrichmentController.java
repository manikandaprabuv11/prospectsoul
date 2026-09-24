package com.vyoog.prospectsoul_backend.enrichment.framework.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.dto.PageResponse;
import com.vyoog.prospectsoul_backend.common.security.CurrentUser;
import com.vyoog.prospectsoul_backend.common.security.RoleConstants;
import com.vyoog.prospectsoul_backend.enrichment.framework.dto.request.EnrichCompanyRequest;
import com.vyoog.prospectsoul_backend.enrichment.framework.dto.request.ResolveCandidateRequest;
import com.vyoog.prospectsoul_backend.enrichment.framework.dto.response.EnrichmentCandidateResponse;
import com.vyoog.prospectsoul_backend.enrichment.framework.dto.response.EnrichmentJobResponse;
import com.vyoog.prospectsoul_backend.enrichment.framework.entity.EnrichmentCandidateEntity;
import com.vyoog.prospectsoul_backend.enrichment.framework.entity.EnrichmentJobEntity;
import com.vyoog.prospectsoul_backend.enrichment.framework.mapper.EnrichmentMapper;
import com.vyoog.prospectsoul_backend.enrichment.framework.service.CandidateService;
import com.vyoog.prospectsoul_backend.enrichment.framework.service.EnrichmentJobService;
import com.vyoog.prospectsoul_backend.enrichment.framework.service.EnrichmentOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EnrichmentController {

    private final EnrichmentOrchestrator orchestrator;
    private final EnrichmentJobService jobService;
    private final CandidateService candidateService;
    private final EnrichmentMapper mapper;

    @PostMapping("/companies/{companyId}/enrich")
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public List<EnrichmentJobResponse> enrichCompany(
            @PathVariable UUID companyId,
            @Valid @RequestBody EnrichCompanyRequest request,
            Authentication auth) {
        UUID userId = UUID.fromString(CurrentUser.id(auth));
        List<EnrichmentJobEntity> jobs = orchestrator.enrichMultipleProviders(
                companyId,
                request.provider_keys(),
                request.force(),
                userId,
                "API"
        );
        return jobs.stream().map(mapper::toJobResponse).toList();
    }

    @GetMapping("/companies/{companyId}/enrichment-jobs")
    @PreAuthorize(RoleConstants.HAS_READ)
    public PageResponse<EnrichmentJobResponse> listJobsByCompany(
            @PathVariable UUID companyId,
            @PageableDefault(size = 25) Pageable pageable) {
        Page<EnrichmentJobResponse> page = jobService.listByCompany(companyId, pageable)
                .map(mapper::toJobResponse);
        return PageResponse.from(page);
    }

    @GetMapping("/enrichment-jobs")
    @PreAuthorize(RoleConstants.HAS_READ)
    public PageResponse<EnrichmentJobResponse> listAllJobs(
            @PageableDefault(size = 25) Pageable pageable) {
        Page<EnrichmentJobResponse> page = jobService.listAll(pageable)
                .map(mapper::toJobResponse);
        return PageResponse.from(page);
    }

    @GetMapping("/enrichment-jobs/{jobId}")
    @PreAuthorize(RoleConstants.HAS_READ)
    public EnrichmentJobResponse getJob(@PathVariable UUID jobId) {
        return mapper.toJobResponse(jobService.getById(jobId));
    }

    @GetMapping("/companies/{companyId}/enrichment-candidates")
    @PreAuthorize(RoleConstants.HAS_READ)
    public PageResponse<EnrichmentCandidateResponse> listCandidates(
            @PathVariable UUID companyId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 25) Pageable pageable) {
        Page<EnrichmentCandidateResponse> page = candidateService.listByCompany(companyId, status, pageable)
                .map(mapper::toCandidateResponse);
        return PageResponse.from(page);
    }

    @PostMapping("/enrichment-candidates/{candidateId}/resolve")
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    public EnrichmentCandidateResponse resolveCandidate(
            @PathVariable UUID candidateId,
            @Valid @RequestBody ResolveCandidateRequest request,
            Authentication auth) {
        String actor = CurrentUser.id(auth);
        return mapper.toCandidateResponse(
                candidateService.resolve(candidateId, request.action(), actor));
    }
}
