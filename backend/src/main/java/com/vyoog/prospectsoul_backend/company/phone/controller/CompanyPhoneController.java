package com.vyoog.prospectsoul_backend.company.phone.controller;

import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.security.CurrentUser;
import com.vyoog.prospectsoul_backend.common.security.RoleConstants;
import com.vyoog.prospectsoul_backend.company.phone.dto.request.CompanyPhoneRequest;
import com.vyoog.prospectsoul_backend.company.phone.dto.request.ConfidenceOverrideRequest;
import com.vyoog.prospectsoul_backend.company.phone.dto.response.CompanyPhoneResponse;
import com.vyoog.prospectsoul_backend.company.phone.service.CompanyPhoneService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CompanyPhoneController {

    private final CompanyPhoneService phoneService;

    @GetMapping("/api/v1/companies/{companyId}/phones")
    @PreAuthorize(RoleConstants.HAS_READ)
    public List<CompanyPhoneResponse> listPhones(@PathVariable UUID companyId) {
        return phoneService.listForCompany(companyId);
    }

    @PostMapping("/api/v1/companies/{companyId}/phones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    public CompanyPhoneResponse addPhone(@PathVariable UUID companyId,
                                          @Valid @RequestBody CompanyPhoneRequest request,
                                          Authentication auth) {
        return phoneService.addPhone(companyId, request, CurrentUser.id(auth));
    }

    @PatchMapping("/api/v1/company-phones/{phoneId}")
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    public CompanyPhoneResponse updatePhone(@PathVariable UUID phoneId,
                                             @Valid @RequestBody CompanyPhoneRequest request,
                                             Authentication auth) {
        return phoneService.updatePhone(phoneId, request, CurrentUser.id(auth));
    }

    @DeleteMapping("/api/v1/company-phones/{phoneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    public void deletePhone(@PathVariable UUID phoneId,
                             Authentication auth) {
        phoneService.removePhone(phoneId, CurrentUser.id(auth));
    }

    @PostMapping("/api/v1/company-phones/{phoneId}/override-confidence")
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    public CompanyPhoneResponse overrideConfidence(@PathVariable UUID phoneId,
                                                    @Valid @RequestBody ConfidenceOverrideRequest request,
                                                    Authentication auth) {
        return phoneService.overrideConfidence(phoneId, request, CurrentUser.id(auth));
    }
}
