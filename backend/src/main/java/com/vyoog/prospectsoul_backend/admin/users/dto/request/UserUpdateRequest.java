package com.vyoog.prospectsoul_backend.admin.users.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(max = 255) String fullName,
        @Email @Size(max = 255) String email,
        @Size(max = 50) String role
) {}
