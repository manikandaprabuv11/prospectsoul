package com.vyoog.prospectsoul_backend.company.dto.response;

import java.util.List;
import java.util.UUID;

public record CompanyIdsResponse(
        List<UUID> ids,
        long total,
        boolean capped
) {}
