package com.vyoog.prospectsoul_backend.imports.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImportTemplateResponse(
        UUID id,
        String name,
        String source,
        Boolean isDefault,
        List<MappingEntry> mappings,
        Instant createdAt
) {
    public record MappingEntry(
            UUID id,
            String sourceHeader,
            String targetField,
            Boolean isActive
    ) {}
}
