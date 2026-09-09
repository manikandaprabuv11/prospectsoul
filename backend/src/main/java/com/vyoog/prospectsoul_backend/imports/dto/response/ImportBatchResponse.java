package com.vyoog.prospectsoul_backend.imports.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ImportBatchResponse(
        UUID id,
        String fileName,
        String fileType,
        String source,
        String status,
        Integer totalRows,
        Integer processedRows,
        Integer createdRows,
        Integer duplicateRows,
        Integer rejectedRows,
        String errorMessage,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {}
