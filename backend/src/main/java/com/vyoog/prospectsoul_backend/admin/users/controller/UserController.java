package com.vyoog.prospectsoul_backend.admin.users.controller;

import java.util.UUID;

import com.vyoog.prospectsoul_backend.admin.users.dto.request.UserCreateRequest;
import com.vyoog.prospectsoul_backend.admin.users.dto.request.UserUpdateRequest;
import com.vyoog.prospectsoul_backend.admin.users.dto.response.UserResponse;
import com.vyoog.prospectsoul_backend.admin.users.service.UserService;
import com.vyoog.prospectsoul_backend.common.dto.PageResponse;
import com.vyoog.prospectsoul_backend.common.security.CurrentUser;
import com.vyoog.prospectsoul_backend.common.security.RoleConstants;
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
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_CONFIGURE)
public class UserController {

    private final UserService userService;

    @GetMapping
    public PageResponse<UserResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "fullName") String sort,
            @RequestParam(name = "sort_dir", defaultValue = "asc") String sortDir) {
        return userService.list(q, role, active, page, size, sort, sortDir);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable UUID id) {
        return userService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserCreateRequest request,
                                Authentication auth) {
        return userService.create(request, CurrentUser.id(auth));
    }

    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable UUID id,
                                @Valid @RequestBody UserUpdateRequest request,
                                Authentication auth) {
        return userService.update(id, request, CurrentUser.id(auth));
    }

    @PostMapping("/{id}/activate")
    public UserResponse activate(@PathVariable UUID id, Authentication auth) {
        return userService.activate(id, CurrentUser.id(auth));
    }

    @PostMapping("/{id}/deactivate")
    public UserResponse deactivate(@PathVariable UUID id, Authentication auth) {
        return userService.deactivate(id, CurrentUser.id(auth));
    }
}
