package com.vyoog.prospectsoul_backend.imports.mapper;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.vyoog.prospectsoul_backend.imports.dto.response.ImportBatchResponse;
import com.vyoog.prospectsoul_backend.imports.dto.response.ImportRowResponse;
import com.vyoog.prospectsoul_backend.imports.dto.response.ImportTemplateResponse;
import com.vyoog.prospectsoul_backend.imports.entity.ImportBatch;
import com.vyoog.prospectsoul_backend.imports.entity.ImportRow;
import com.vyoog.prospectsoul_backend.imports.entity.ImportTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImportMapper {

    private final ObjectMapper objectMapper;

    public ImportBatchResponse toBatchResponse(ImportBatch batch) {
        return new ImportBatchResponse(
                batch.getId(),
                batch.getFileName(),
                batch.getFileType(),
                batch.getSource(),
                batch.getStatus().name(),
                batch.getTotalRows(),
                batch.getProcessedRows(),
                batch.getCreatedRows(),
                batch.getDuplicateRows(),
                batch.getRejectedRows(),
                batch.getErrorMessage(),
                batch.getCreatedBy(),
                batch.getCreatedAt(),
                batch.getUpdatedAt()
        );
    }

    public ImportRowResponse toRowResponse(ImportRow row) {
        return new ImportRowResponse(
                row.getId(),
                row.getRowNumber(),
                parseJson(row.getRawData()),
                parseJson(row.getMappedData()),
                row.getStatus().name(),
                row.getErrorMessage(),
                row.getCompanyId(),
                row.getDuplicateOfCompanyId(),
                row.getCreatedAt()
        );
    }

    public ImportTemplateResponse toTemplateResponse(ImportTemplate template) {
        return new ImportTemplateResponse(
                template.getId(),
                template.getName(),
                template.getSource(),
                template.getIsDefault(),
                template.getMappings().stream()
                        .map(m -> new ImportTemplateResponse.MappingEntry(
                                m.getId(), m.getSourceHeader(), m.getTargetField(), m.getIsActive()))
                        .toList(),
                template.getCreatedAt()
        );
    }

    private Object parseJson(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JacksonException e) {
            return json;
        }
    }
}
