package com.vyoog.prospectsoul_backend.imports.dto.response;

import java.util.List;

public record MappingSuggestionResponse(
        List<String> detectedHeaders,
        List<Suggestion> suggestions,
        List<String> availableTargetFields
) {
    public record Suggestion(
            String sourceColumn,
            String targetField,
            double confidence,
            String matchType,
            boolean ambiguous
    ) {}
}
