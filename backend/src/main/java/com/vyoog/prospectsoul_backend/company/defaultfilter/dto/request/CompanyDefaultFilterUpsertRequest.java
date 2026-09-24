package com.vyoog.prospectsoul_backend.company.defaultfilter.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompanyDefaultFilterUpsertRequest(
        @NotBlank @Size(max = 80) String filterKey,
        @NotBlank @Size(max = 120) String label,
        @NotBlank
        @Pattern(regexp = "^(eq|ne|gte|lte|gt|lt|between|in|is_present|is_missing)$",
                 message = "operator must be one of eq|ne|gte|lte|gt|lt|between|in|is_present|is_missing")
        String operator,
        // JSON value — scalar, [min,max], or array. Optional (some operators
        // like is_present / is_missing take no value).
        String value,
        Boolean active,
        Short sortOrder
) {}
