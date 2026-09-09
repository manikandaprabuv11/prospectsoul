package com.vyoog.prospectsoul_backend.company.controller;

import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.dto.PageResponse;
import com.vyoog.prospectsoul_backend.common.security.CurrentUser;
import com.vyoog.prospectsoul_backend.common.security.RoleConstants;
import com.vyoog.prospectsoul_backend.company.dto.request.CompanyCreateRequest;
import com.vyoog.prospectsoul_backend.company.dto.request.CompanyUpdateRequest;
import com.vyoog.prospectsoul_backend.company.dto.response.CompanyResponse;
import com.vyoog.prospectsoul_backend.company.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    public CompanyResponse create(@Valid @RequestBody CompanyCreateRequest request,
                                   Authentication auth) {
        return companyService.create(request, CurrentUser.id(auth));
    }

    @GetMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_READ)
    public CompanyResponse getById(@PathVariable UUID id) {
        return companyService.getById(id);
    }

    @GetMapping
    @PreAuthorize(RoleConstants.HAS_READ)
    public PageResponse<CompanyResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String cluster,
            @RequestParam(required = false) String source,
            @RequestParam(name = "pipeline_state", required = false) String pipelineState,
            @RequestParam(name = "verification_status", required = false) String verificationStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(name = "sort_dir", defaultValue = "desc") String sortDir) {
        return companyService.list(q, city, state, industry, cluster, source,
                pipelineState, verificationStatus, page, size, sort, sortDir);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    public CompanyResponse update(@PathVariable UUID id,
                                   @Valid @RequestBody CompanyUpdateRequest request,
                                   Authentication auth) {
        return companyService.update(id, request, CurrentUser.id(auth));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    public CompanyResponse verify(@PathVariable UUID id,
                                   Authentication auth) {
        return companyService.verify(id, CurrentUser.id(auth));
    }
}
