package com.vyoog.prospectsoul_backend.contact.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ContactResponse(
        UUID id,
        UUID companyId,
        String name,
        String designation,
        String phone,
        String email,
        UUID roleId,
        String roleKey,
        String roleLabel,
        Boolean isPrimary,
        Boolean isMdOwner,
        LocalDate associationStart,
        LocalDate associationEnd,
        String verificationStatus,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt
) {}
