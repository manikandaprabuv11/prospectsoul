package com.vyoog.prospectsoul_backend.admin.designation.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record DesignationCreateRequest(
        @NotBlank(message = "Designation is required")
        String designation,
        List<String> aliases
) {}
