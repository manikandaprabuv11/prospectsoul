package com.vyoog.prospectsoul_backend.company.defaultfilter.controller;

import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.security.CurrentUser;
import com.vyoog.prospectsoul_backend.common.security.RoleConstants;
import com.vyoog.prospectsoul_backend.company.defaultfilter.dto.request.CompanyDefaultFilterUpsertRequest;
import com.vyoog.prospectsoul_backend.company.defaultfilter.dto.response.CompanyDefaultFilterResponse;
import com.vyoog.prospectsoul_backend.company.defaultfilter.service.CompanyDefaultFilterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/company-default-filters")
@RequiredArgsConstructor
public class CompanyDefaultFilterController {

    private final CompanyDefaultFilterService service;

    @GetMapping
    @PreAuthorize(RoleConstants.HAS_READ)
    public List<CompanyDefaultFilterResponse> list(
            @RequestParam(name = "include_inactive", defaultValue = "true") boolean includeInactive) {
        return service.list(includeInactive);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(RoleConstants.HAS_CONFIGURE)
    public CompanyDefaultFilterResponse create(@Valid @RequestBody CompanyDefaultFilterUpsertRequest req,
                                                Authentication auth) {
        return service.create(req, CurrentUser.id(auth));
    }

    @PatchMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_CONFIGURE)
    public CompanyDefaultFilterResponse update(@PathVariable UUID id,
                                                @Valid @RequestBody CompanyDefaultFilterUpsertRequest req,
                                                Authentication auth) {
        return service.update(id, req, CurrentUser.id(auth));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(RoleConstants.HAS_CONFIGURE)
    public void delete(@PathVariable UUID id, Authentication auth) {
        service.delete(id, CurrentUser.id(auth));
    }
}
