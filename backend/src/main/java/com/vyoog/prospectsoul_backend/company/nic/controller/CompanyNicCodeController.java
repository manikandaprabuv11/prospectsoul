package com.vyoog.prospectsoul_backend.company.nic.controller;

import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.security.CurrentUser;
import com.vyoog.prospectsoul_backend.common.security.RoleConstants;
import com.vyoog.prospectsoul_backend.company.nic.dto.request.AttachNicCodeRequest;
import com.vyoog.prospectsoul_backend.company.nic.dto.response.CompanyNicCodeResponse;
import com.vyoog.prospectsoul_backend.company.nic.service.CompanyNicCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/nic-codes")
@RequiredArgsConstructor
public class CompanyNicCodeController {

    private final CompanyNicCodeService service;

    @GetMapping
    @PreAuthorize(RoleConstants.HAS_READ)
    public List<CompanyNicCodeResponse> list(@PathVariable UUID companyId) {
        return service.list(companyId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    public CompanyNicCodeResponse attach(@PathVariable UUID companyId,
                                          @Valid @RequestBody AttachNicCodeRequest req,
                                          Authentication auth) {
        return service.attach(companyId, req, CurrentUser.id(auth));
    }

    @PostMapping("/{rowId}/make-primary")
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    public CompanyNicCodeResponse makePrimary(@PathVariable UUID companyId,
                                               @PathVariable UUID rowId,
                                               Authentication auth) {
        return service.makePrimary(companyId, rowId, CurrentUser.id(auth));
    }

    @DeleteMapping("/{rowId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    public void detach(@PathVariable UUID companyId,
                        @PathVariable UUID rowId,
                        Authentication auth) {
        service.detach(companyId, rowId, CurrentUser.id(auth));
    }
}
