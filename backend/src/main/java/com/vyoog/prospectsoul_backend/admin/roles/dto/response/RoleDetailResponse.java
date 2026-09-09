package com.vyoog.prospectsoul_backend.admin.roles.dto.response;

import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.admin.users.dto.response.UserResponse;

public record RoleDetailResponse(
        UUID id,
        String name,
        String displayName,
        String description,
        List<String> permissions,
        long userCount,
        boolean active,
        List<UserResponse> assignedUsers
) {}
