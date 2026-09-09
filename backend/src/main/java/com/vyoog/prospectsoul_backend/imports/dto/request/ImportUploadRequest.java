package com.vyoog.prospectsoul_backend.imports.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ImportUploadRequest(
        @NotBlank String source
) {}
