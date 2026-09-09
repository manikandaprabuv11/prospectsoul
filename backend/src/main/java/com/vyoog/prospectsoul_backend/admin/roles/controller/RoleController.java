package com.vyoog.prospectsoul_backend.admin.roles.controller;

import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.admin.roles.dto.request.AssignUserRequest;
import com.vyoog.prospectsoul_backend.admin.roles.dto.request.RoleUpdateRequest;
import com.vyoog.prospectsoul_backend.admin.roles.dto.response.RoleDetailResponse;
import com.vyoog.prospectsoul_backend.admin.roles.dto.response.RoleResponse;
import com.vyoog.prospectsoul_backend.admin.roles.service.RoleService;
import com.vyoog.prospectsoul_backend.admin.users.dto.response.UserResponse;
import com.vyoog.prospectsoul_backend.common.security.CurrentUser;
import com.vyoog.prospectsoul_backend.common.security.RoleConstants;
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
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_CONFIGURE)
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public List<RoleResponse> list() {
        return roleService.list();
    }

    @GetMapping("/{id}")
    public RoleDetailResponse getById(@PathVariable UUID id) {
        return roleService.getById(id);
    }

    @PatchMapping("/{id}")
    public RoleResponse update(@PathVariable UUID id,
                                @Valid @RequestBody RoleUpdateRequest request,
                                Authentication auth) {
        return roleService.update(id, request, CurrentUser.id(auth));
    }

    @GetMapping("/{id}/users")
    public List<UserResponse> getAssignedUsers(@PathVariable UUID id) {
        return roleService.getAssignedUsers(id);
    }

    @PostMapping("/{id}/users")
    @ResponseStatus(HttpStatus.CREATED)
    public void assignUser(@PathVariable UUID id,
                            @Valid @RequestBody AssignUserRequest request,
                            Authentication auth) {
        roleService.assignUser(id, request.userId(), CurrentUser.id(auth));
    }

    @DeleteMapping("/{id}/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassignUser(@PathVariable UUID id,
                              @PathVariable UUID userId,
                              Authentication auth) {
        roleService.unassignUser(id, userId, CurrentUser.id(auth));
    }
}
