package com.vyoog.prospectsoul_backend.admin.designation.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DesignationResponse(
        UUID id,
        String designation,
        List<String> aliases,
        Boolean active,
        Instant createdAt
) {}
