package com.vyoog.prospectsoul_backend.company.controller;

import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.dto.PageResponse;
import com.vyoog.prospectsoul_backend.common.security.CurrentUser;
import com.vyoog.prospectsoul_backend.common.security.RoleConstants;
import com.vyoog.prospectsoul_backend.company.dto.request.CompanyCreateRequest;
import com.vyoog.prospectsoul_backend.company.dto.request.CompanyUpdateRequest;
import com.vyoog.prospectsoul_backend.company.dto.response.CompanyGroupedByNicResponse;
import com.vyoog.prospectsoul_backend.company.dto.response.CompanyResponse;
import com.vyoog.prospectsoul_backend.company.specification.CompanySpecification;
import java.math.BigDecimal;
import java.util.List;
import com.vyoog.prospectsoul_backend.company.download.CompanyDownloadRequest;
import com.vyoog.prospectsoul_backend.company.download.CompanyDownloadService;
import com.vyoog.prospectsoul_backend.company.service.CompanyService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final CompanyDownloadService companyDownloadService;

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
    public Object list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String cluster,
            @RequestParam(required = false) String source,
            @RequestParam(name = "pipeline_state", required = false) String pipelineState,
            @RequestParam(name = "verification_status", required = false) String verificationStatus,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String pincode,
            @RequestParam(name = "turnover_min", required = false) BigDecimal turnoverMin,
            @RequestParam(name = "turnover_max", required = false) BigDecimal turnoverMax,
            @RequestParam(name = "employee_min", required = false) Integer employeeMin,
            @RequestParam(name = "employee_max", required = false) Integer employeeMax,
            @RequestParam(name = "gst_present", required = false) Boolean gstPresent,
            @RequestParam(name = "nic_code_id", required = false) UUID nicCodeId,
            @RequestParam(name = "nic_parent_id", required = false) UUID nicParentId,
            @RequestParam(name = "nic_include_descendants", required = false) Boolean nicIncludeDescendants,
            @RequestParam(name = "has_contact_role_id", required = false) UUID hasContactRoleId,
            @RequestParam(name = "view", defaultValue = "flat") String view,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(name = "sort_dir", defaultValue = "desc") String sortDir) {

        java.util.Collection<UUID> nicIds = null;
        if (nicParentId != null) {
            // Descendants default ON when a parent is given (docs 21 §4.2).
            boolean includeDesc = nicIncludeDescendants == null || nicIncludeDescendants;
            nicIds = includeDesc ? companyService.expandNicSubtree(nicParentId) : List.of(nicParentId);
        }

        CompanySpecification.Filters filters = new CompanySpecification.Filters(
                q, city, state, industry, cluster, source,
                pipelineState, verificationStatus,
                region, district, pincode,
                turnoverMin, turnoverMax, employeeMin, employeeMax, gstPresent,
                nicCodeId, nicIds, hasContactRoleId);

        if ("grouped_by_nic".equalsIgnoreCase(view)) {
            if (nicParentId == null) {
                throw new com.vyoog.prospectsoul_backend.common.exception.BusinessRuleException(
                        "view=grouped_by_nic requires nic_parent_id");
            }
            return companyService.groupedByNic(nicParentId, filters);
        }
        return companyService.listWithFilters(filters, page, size, sort, sortDir);
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


    @PostMapping("/download")
    @PreAuthorize(RoleConstants.HAS_READ)
    public ResponseEntity<ByteArrayResource> download(@Valid @RequestBody CompanyDownloadRequest request,
                                                        Authentication auth) {
        CompanyDownloadService.DownloadResult result = companyDownloadService.download(request, CurrentUser.id(auth));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(new ByteArrayResource(result.bytes()));
    }
}
