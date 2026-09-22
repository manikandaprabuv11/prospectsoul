package com.vyoog.prospectsoul_backend.contactrole.controller;

import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.security.CurrentUser;
import com.vyoog.prospectsoul_backend.common.security.RoleConstants;
import com.vyoog.prospectsoul_backend.contactrole.dto.request.ContactRoleCreateRequest;
import com.vyoog.prospectsoul_backend.contactrole.dto.request.ContactRoleUpdateRequest;
import com.vyoog.prospectsoul_backend.contactrole.dto.response.ContactRoleResponse;
import com.vyoog.prospectsoul_backend.contactrole.service.ContactRoleService;
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
@RequestMapping("/api/v1/contact-roles")
@RequiredArgsConstructor
public class ContactRoleController {

    private final ContactRoleService contactRoleService;

    @GetMapping
    @PreAuthorize(RoleConstants.HAS_READ)
    public List<ContactRoleResponse> list(
            @RequestParam(name = "include_inactive", defaultValue = "false") boolean includeInactive) {
        return contactRoleService.list(includeInactive);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(RoleConstants.HAS_CONFIGURE)
    public ContactRoleResponse create(@Valid @RequestBody ContactRoleCreateRequest req,
                                       Authentication auth) {
        return contactRoleService.create(req, CurrentUser.id(auth));
    }

    @PatchMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_CONFIGURE)
    public ContactRoleResponse update(@PathVariable UUID id,
                                       @Valid @RequestBody ContactRoleUpdateRequest req,
                                       Authentication auth) {
        return contactRoleService.update(id, req, CurrentUser.id(auth));
    }
}
