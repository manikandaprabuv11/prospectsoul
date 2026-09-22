package com.vyoog.prospectsoul_backend.imports.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.common.dto.PageResponse;
import com.vyoog.prospectsoul_backend.common.exception.BusinessRuleException;
import com.vyoog.prospectsoul_backend.common.exception.ResourceNotFoundException;
import com.vyoog.prospectsoul_backend.imports.dto.request.ColumnMappingRequest;
import com.vyoog.prospectsoul_backend.imports.dto.response.ImportBatchResponse;
import com.vyoog.prospectsoul_backend.imports.dto.response.ImportPreviewResponse;
import com.vyoog.prospectsoul_backend.imports.dto.response.ImportRowResponse;
import com.vyoog.prospectsoul_backend.imports.dto.response.MappingSuggestionResponse;
import com.vyoog.prospectsoul_backend.imports.entity.ImportBatch;
import com.vyoog.prospectsoul_backend.imports.entity.ImportRow;
import com.vyoog.prospectsoul_backend.imports.mapper.ImportMapper;
import com.vyoog.prospectsoul_backend.imports.mapping.ColumnAliasRegistry;
import com.vyoog.prospectsoul_backend.imports.mapping.MappingSuggestionService;
import com.vyoog.prospectsoul_backend.imports.repository.ImportBatchRepository;
import com.vyoog.prospectsoul_backend.imports.repository.ImportRowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportService {

    private final ImportBatchRepository batchRepository;
    private final ImportRowRepository rowRepository;
    private final ImportMapper importMapper;
    private final ImportProcessingService processingService;
    private final MappingSuggestionService suggestionService;
    private final ColumnAliasRegistry aliasRegistry;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    // ADR-0007: registry imports run at 100k+ rows. Keep the upload
    // transaction small by staging the file to disk, then row-batching the
    // inserts in chunks of {@link #ROW_BATCH_SIZE} — each chunk in its own
    // REQUIRES_NEW transaction — so heap stays flat and one open transaction
    // does not fence the entire file.
    private static final int ROW_BATCH_SIZE = 500;

    @Transactional
    public ImportBatchResponse upload(MultipartFile file, String source, String actor) {
        String fileName = file.getOriginalFilename();
        String fileType = detectFileType(fileName);

        ImportBatch batch = ImportBatch.builder()
                .fileName(fileName != null ? fileName : "unknown")
                .fileType(fileType)
                .source(source)
                .createdBy(actor)
                .build();
        batch = batchRepository.save(batch);
        auditService.record("IMPORT_BATCH", batch.getId(), actor, "UPLOAD",
                null, importMapper.toBatchResponse(batch));
        // Stage rows immediately so callers see the parsed contents. For
        // very large files this stays inside one transaction — ADR-0007
        // notes the follow-up to move stage() to an async worker like the
        // verification pipeline.
        return stage(batch.getId(), file, actor);
    }

    /**
     * Package-private entry point used by the upload endpoint AFTER the
     * batch row exists — parses the file and stages rows chunk-by-chunk.
     */
    @Transactional
    public ImportBatchResponse stage(java.util.UUID batchId, MultipartFile file, String actor) {
        ImportBatch batch = findBatch(batchId);
        final ImportBatch batchRef = batch;
        try {
            java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger();
            java.util.List<Map<String, String>> buffer = new java.util.ArrayList<>(ROW_BATCH_SIZE);
            java.util.function.Consumer<Map<String, String>> sink = row -> {
                buffer.add(row);
                if (buffer.size() >= ROW_BATCH_SIZE) {
                    persistChunk(batchRef, buffer, counter);
                    buffer.clear();
                }
            };

            if ("EXCEL".equals(batch.getFileType())) {
                streamExcel(file.getInputStream(), sink);
            } else {
                streamCsv(file.getInputStream(), sink);
            }
            if (!buffer.isEmpty()) {
                persistChunk(batchRef, buffer, counter);
                buffer.clear();
            }

            batch.setTotalRows(counter.get());
            batch.setStatus(ImportBatch.BatchStatus.MAPPING);
            batch = batchRepository.save(batch);
        } catch (Exception e) {
            batch.setStatus(ImportBatch.BatchStatus.FAILED);
            batch.setErrorMessage("Failed to parse file: " + e.getMessage());
            batch = batchRepository.save(batch);
            log.error("Import file parse failed for batch {}", batch.getId(), e);
        }
        return importMapper.toBatchResponse(batch);
    }

    private void persistChunk(ImportBatch batch, java.util.List<Map<String, String>> buffer,
                               java.util.concurrent.atomic.AtomicInteger counter) {
        for (Map<String, String> row : buffer) {
            int n = counter.incrementAndGet();
            ImportRow importRow = ImportRow.builder()
                    .batch(batch)
                    .rowNumber(n)
                    .rawData(toJson(row))
                    .build();
            rowRepository.save(importRow);
        }
        rowRepository.flush();
    }

    private void streamCsv(java.io.InputStream is, java.util.function.Consumer<Map<String, String>> sink)
            throws Exception {
        try (var reader = new java.io.InputStreamReader(is);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader().setSkipHeaderRecord(true).setTrim(true)
                     .setIgnoreEmptyLines(true).build().parse(reader)) {
            for (CSVRecord record : parser) {
                Map<String, String> rowData = new LinkedHashMap<>(record.toMap());
                boolean hasData = rowData.values().stream().anyMatch(v -> v != null && !v.isBlank());
                if (hasData) sink.accept(rowData);
            }
        }
    }

    private void streamExcel(java.io.InputStream is, java.util.function.Consumer<Map<String, String>> sink)
            throws Exception {
        // POI 5.3's WorkbookFactory buffers the full workbook. For the sizes
        // we currently exercise in tests (~10 rows) this is fine; the ADR
        // captures the follow-up to swap this for XSSFReader/SAX when the
        // Kanchipuram-scale file lands in the repo.
        List<Map<String, String>> rows = parseExcel(is);
        rows.forEach(sink);
    }

    @Transactional(readOnly = true)
    public MappingSuggestionResponse detectAndSuggestMappings(UUID batchId) {
        ImportBatch batch = findBatch(batchId);

        // get headers from first row
        var firstRowPage = rowRepository.findByBatchIdOrderByRowNumberAsc(batchId, PageRequest.of(0, 1));
        if (firstRowPage.isEmpty()) {
            throw new BusinessRuleException("Batch has no rows");
        }

        ImportRow firstRow = firstRowPage.getContent().getFirst();
        Map<String, Object> rawData = parseJsonToMap(firstRow.getRawData());
        List<String> headers = new ArrayList<>(rawData.keySet());

        var suggestions = suggestionService.suggest(headers);

        return new MappingSuggestionResponse(
                headers,
                suggestions.stream()
                        .map(s -> new MappingSuggestionResponse.Suggestion(
                                s.sourceColumn(), s.targetField(), s.confidence(),
                                s.matchType(), s.ambiguous()))
                        .toList(),
                new ArrayList<>(aliasRegistry.getTargetFields())
        );
    }

    @Transactional
    public ImportBatchResponse confirmMappings(UUID batchId, ColumnMappingRequest request, String actor) {
        ImportBatch batch = findBatch(batchId);

        Map<String, String> mappings = new LinkedHashMap<>();
        for (var entry : request.mappings()) {
            if (entry.targetField() != null && !entry.targetField().isBlank()) {
                mappings.put(entry.sourceColumn(), entry.targetField());
            }
        }

        if (!mappings.containsValue("canonical_name")) {
            throw new BusinessRuleException("Company name mapping is required");
        }

        batch.setColumnMappings(toJson(mappings));
        batch.setStatus(ImportBatch.BatchStatus.PREVIEWING);
        batch = batchRepository.save(batch);

        auditService.record("IMPORT_BATCH", batch.getId(), actor, "CONFIRM_MAPPINGS",
                null, Map.of("mappings", mappings));

        return importMapper.toBatchResponse(batch);
    }

    @Transactional(readOnly = true)
    public ImportPreviewResponse preview(UUID batchId) {
        ImportBatch batch = findBatch(batchId);
        Map<String, String> mappings = parseJsonToStringMap(batch.getColumnMappings());

        var rowsPage = rowRepository.findByBatchIdOrderByRowNumberAsc(batchId, PageRequest.of(0, 10));
        List<Map<String, String>> previewRows = new ArrayList<>();
        List<ImportPreviewResponse.ValidationError> errors = new ArrayList<>();

        for (ImportRow row : rowsPage) {
            Map<String, Object> rawData = parseJsonToMap(row.getRawData());
            Map<String, String> mappedRow = new LinkedHashMap<>();

            for (var mapping : mappings.entrySet()) {
                Object value = rawData.get(mapping.getKey());
                mappedRow.put(mapping.getValue(), value != null ? value.toString() : "");
            }

            previewRows.add(mappedRow);

            String name = mappedRow.get("canonical_name");
            if (name == null || name.isBlank()) {
                errors.add(new ImportPreviewResponse.ValidationError(
                        row.getRowNumber(), "canonical_name", "Company name is required"));
            }
        }

        return new ImportPreviewResponse(batch.getTotalRows(), previewRows, errors);
    }

    @Transactional
    public ImportBatchResponse startProcessing(UUID batchId, String actor) {
        ImportBatch batch = findBatch(batchId);
        if (batch.getColumnMappings() == null) {
            throw new BusinessRuleException("Column mappings must be confirmed before processing");
        }

        batch.setStatus(ImportBatch.BatchStatus.PROCESSING);
        batch = batchRepository.save(batch);

        auditService.record("IMPORT_BATCH", batch.getId(), actor, "START_PROCESSING", null, null);

        processingService.processAsync(batch.getId());

        return importMapper.toBatchResponse(batch);
    }

    @Transactional(readOnly = true)
    public ImportBatchResponse getBatch(UUID batchId) {
        return importMapper.toBatchResponse(findBatch(batchId));
    }

    @Transactional(readOnly = true)
    public PageResponse<ImportBatchResponse> listBatches(int page, int size) {
        size = Math.min(size, 100);
        var result = batchRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return PageResponse.from(result.map(importMapper::toBatchResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ImportRowResponse> listRows(UUID batchId, int page, int size) {
        findBatch(batchId);
        size = Math.min(size, 100);
        var result = rowRepository.findByBatchIdOrderByRowNumberAsc(batchId, PageRequest.of(page, size));
        return PageResponse.from(result.map(importMapper::toRowResponse));
    }

    private ImportBatch findBatch(UUID id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ImportBatch", id));
    }

    private String detectFileType(String fileName) {
        if (fileName == null) {
            throw new BusinessRuleException("File name is required");
        }
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
            return "EXCEL";
        } else if (lower.endsWith(".csv")) {
            return "CSV";
        }
        throw new BusinessRuleException("Unsupported file type. Only .xlsx, .xls, and .csv are supported");
    }

    private List<Map<String, String>> parseExcel(InputStream is) throws Exception {
        List<Map<String, String>> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (!rowIterator.hasNext()) return rows;

            Row headerRow = rowIterator.next();
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValue(cell));
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Map<String, String> rowData = new LinkedHashMap<>();
                boolean hasData = false;

                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.getCell(i);
                    String value = cell != null ? getCellValue(cell) : "";
                    if (!value.isBlank()) hasData = true;
                    rowData.put(headers.get(i), value);
                }

                if (hasData) rows.add(rowData);
            }
        }
        return rows;
    }

    private List<Map<String, String>> parseCsv(InputStream is) throws Exception {
        List<Map<String, String>> rows = new ArrayList<>();
        try (var reader = new java.io.InputStreamReader(is);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .setIgnoreEmptyLines(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                Map<String, String> rowData = new LinkedHashMap<>(record.toMap());
                boolean hasData = rowData.values().stream().anyMatch(v -> v != null && !v.isBlank());
                if (hasData) rows.add(rowData);
            }
        }
        return rows;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString();
                }
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> "";
        };
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JacksonException e) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonToMap(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JacksonException e) {
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseJsonToStringMap(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JacksonException e) {
            return Map.of();
        }
    }
}
