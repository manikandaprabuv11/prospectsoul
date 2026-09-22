package com.vyoog.prospectsoul_backend.contact.dto.request;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContactCreateRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String designation,
        @Size(max = 20) String phone,
        @Email @Size(max = 255) String email,
        @NotNull UUID roleId,
        Boolean isPrimary,
        LocalDate associationStart,
        LocalDate associationEnd
) {}
