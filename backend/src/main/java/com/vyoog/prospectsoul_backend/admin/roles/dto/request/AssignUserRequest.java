package com.vyoog.prospectsoul_backend.admin.roles.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AssignUserRequest(
        @NotNull UUID userId
) {}
