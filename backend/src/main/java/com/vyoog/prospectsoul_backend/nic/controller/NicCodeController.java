package com.vyoog.prospectsoul_backend.nic.controller;

import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.dto.PageResponse;
import com.vyoog.prospectsoul_backend.common.security.CurrentUser;
import com.vyoog.prospectsoul_backend.common.security.RoleConstants;
import com.vyoog.prospectsoul_backend.nic.dto.request.NicCodeCreateRequest;
import com.vyoog.prospectsoul_backend.nic.dto.request.NicCodeUpdateRequest;
import com.vyoog.prospectsoul_backend.nic.dto.response.NicCodeResponse;
import com.vyoog.prospectsoul_backend.nic.dto.response.NicTreeNodeResponse;
import com.vyoog.prospectsoul_backend.nic.service.NicCodeService;
import com.vyoog.prospectsoul_backend.nic.service.NicTreeService;
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
@RequestMapping("/api/v1/nic-codes")
@RequiredArgsConstructor
public class NicCodeController {

    private final NicCodeService nicCodeService;
    private final NicTreeService nicTreeService;

    @GetMapping
    @PreAuthorize(RoleConstants.HAS_READ)
    public PageResponse<NicCodeResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Short level,
            @RequestParam(name = "industry_type", required = false) String industryType,
            @RequestParam(name = "is_primary", required = false) Boolean isPrimary,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "code") String sort,
            @RequestParam(name = "sort_dir", defaultValue = "asc") String sortDir) {
        return nicCodeService.list(q, level, industryType, isPrimary, active, page, size, sort, sortDir);
    }

    @GetMapping("/primary")
    @PreAuthorize(RoleConstants.HAS_READ)
    public List<NicCodeResponse> primary() {
        return nicCodeService.listPrimary();
    }

    @GetMapping("/tree")
    @PreAuthorize(RoleConstants.HAS_READ)
    public List<NicTreeNodeResponse> tree(
            @RequestParam(name = "root_id", required = false) UUID rootId,
            @RequestParam(defaultValue = "3") int depth) {
        if (rootId == null) return nicTreeService.fullTree();
        return nicTreeService.subtree(rootId, depth);
    }

    @GetMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_READ)
    public NicCodeResponse getById(@PathVariable UUID id) {
        return nicCodeService.getById(id);
    }

    @GetMapping("/{id}/children")
    @PreAuthorize(RoleConstants.HAS_READ)
    public List<NicCodeResponse> children(@PathVariable UUID id) {
        return nicCodeService.listChildren(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(RoleConstants.HAS_CONFIGURE)
    public NicCodeResponse create(@Valid @RequestBody NicCodeCreateRequest request,
                                   Authentication auth) {
        return nicCodeService.create(request, CurrentUser.id(auth));
    }

    @PatchMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_CONFIGURE)
    public NicCodeResponse update(@PathVariable UUID id,
                                   @Valid @RequestBody NicCodeUpdateRequest request,
                                   Authentication auth) {
        return nicCodeService.update(id, request, CurrentUser.id(auth));
    }

    @PostMapping("/{id}/toggle-primary")
    @PreAuthorize(RoleConstants.HAS_CONFIGURE)
    public NicCodeResponse togglePrimary(@PathVariable UUID id, Authentication auth) {
        return nicCodeService.togglePrimary(id, CurrentUser.id(auth));
    }
}
