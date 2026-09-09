package com.vyoog.prospectsoul_backend.admin.users.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String fullName,
        String email,
        String role,
        String roleDisplayName,
        boolean active,
        Instant lastLoginAt,
        Instant createdAt
) {}
