package com.vyoog.prospectsoul_backend.contact.controller;

import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.security.CurrentUser;
import com.vyoog.prospectsoul_backend.common.security.RoleConstants;
import com.vyoog.prospectsoul_backend.contact.dto.request.ContactCreateRequest;
import com.vyoog.prospectsoul_backend.contact.dto.request.ContactUpdateRequest;
import com.vyoog.prospectsoul_backend.contact.dto.response.ContactResponse;
import com.vyoog.prospectsoul_backend.contact.service.ContactService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping("/companies/{companyId}/contacts")
    @PreAuthorize(RoleConstants.HAS_READ)
    public List<ContactResponse> list(@PathVariable UUID companyId) {
        return contactService.listForCompany(companyId);
    }

    @PostMapping("/companies/{companyId}/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    public ContactResponse create(@PathVariable UUID companyId,
                                   @Valid @RequestBody ContactCreateRequest req,
                                   Authentication auth) {
        return contactService.create(companyId, req, CurrentUser.id(auth));
    }

    @PatchMapping("/contacts/{contactId}")
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    public ContactResponse update(@PathVariable UUID contactId,
                                   @Valid @RequestBody ContactUpdateRequest req,
                                   Authentication auth) {
        return contactService.update(contactId, req, CurrentUser.id(auth));
    }

    @PostMapping("/contacts/{contactId}/make-primary")
    @PreAuthorize(RoleConstants.HAS_MUTATE)
    public ContactResponse makePrimary(@PathVariable UUID contactId, Authentication auth) {
        return contactService.makePrimary(contactId, CurrentUser.id(auth));
    }
}
