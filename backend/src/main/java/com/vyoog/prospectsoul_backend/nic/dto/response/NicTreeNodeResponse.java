package com.vyoog.prospectsoul_backend.nic.dto.response;

import java.util.List;
import java.util.UUID;

public record NicTreeNodeResponse(
        UUID id,
        String code,
        String description,
        String industryType,
        Short level,
        Boolean isPrimary,
        Boolean active,
        long childCount,
        List<NicTreeNodeResponse> children
) {}
