package com.vyoog.prospectsoul_backend.imports.dto.response;

import java.util.List;
import java.util.Map;

public record ImportPreviewResponse(
        int totalRows,
        List<Map<String, String>> previewRows,
        List<ValidationError> validationErrors
) {
    public record ValidationError(
            int rowNumber,
            String field,
            String message
    ) {}
}
