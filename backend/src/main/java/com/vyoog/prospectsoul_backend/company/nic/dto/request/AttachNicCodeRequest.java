package com.vyoog.prospectsoul_backend.company.nic.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AttachNicCodeRequest(
        UUID nicCodeId,
        @Pattern(regexp = "^[0-9]{1,5}$", message = "code must be 1-5 digits")
        String code,
        @Size(max = 500) String descriptionRaw,
        Boolean isPrimary
) {}
