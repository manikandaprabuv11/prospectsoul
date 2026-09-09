package com.vyoog.prospectsoul_backend.company.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyCreateRequest(
        @NotBlank @Size(max = 500) String canonicalName,
        @Size(max = 500) String websiteDomain,
        @Size(max = 20) String primaryPhone,
        @Size(max = 500) String email,
        @Size(max = 200) String city,
        @Size(max = 200) String state,
        @Size(max = 200) String cluster,
        @Size(max = 200) String industry,
        @Size(max = 50) String sizeBand,
        List<String> tags,
        @Size(max = 50) String source
) {}
