package com.vyoog.prospectsoul_backend.imports.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record ColumnMappingRequest(
        @NotEmpty List<MappingEntry> mappings
) {
    public record MappingEntry(
            String sourceColumn,
            String targetField
    ) {}
}
