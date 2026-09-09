package com.vyoog.prospectsoul_backend.imports.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.mapper.CompanyMapper;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.imports.entity.ImportBatch;
import com.vyoog.prospectsoul_backend.imports.entity.ImportRow;
import com.vyoog.prospectsoul_backend.imports.normalization.CompanyNameNormalizer;
import com.vyoog.prospectsoul_backend.imports.normalization.PhoneNormalizer;
import com.vyoog.prospectsoul_backend.imports.normalization.WebsiteNormalizer;
import com.vyoog.prospectsoul_backend.imports.repository.ImportBatchRepository;
import com.vyoog.prospectsoul_backend.imports.repository.ImportRowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ImportProcessingService {

    private final ImportBatchRepository batchRepository;
    private final ImportRowRepository rowRepository;
    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final AuditService auditService;
    private final CompanyNameNormalizer nameNormalizer;
    private final PhoneNormalizer phoneNormalizer;
    private final WebsiteNormalizer websiteNormalizer;
    private final ObjectMapper objectMapper;
    private final ImportProcessingService self;

    public ImportProcessingService(
            ImportBatchRepository batchRepository,
            ImportRowRepository rowRepository,
            CompanyRepository companyRepository,
            CompanyMapper companyMapper,
            AuditService auditService,
            CompanyNameNormalizer nameNormalizer,
            PhoneNormalizer phoneNormalizer,
            WebsiteNormalizer websiteNormalizer,
            ObjectMapper objectMapper,
            @Lazy ImportProcessingService self) {
        this.batchRepository = batchRepository;
        this.rowRepository = rowRepository;
        this.companyRepository = companyRepository;
        this.companyMapper = companyMapper;
        this.auditService = auditService;
        this.nameNormalizer = nameNormalizer;
        this.phoneNormalizer = phoneNormalizer;
        this.websiteNormalizer = websiteNormalizer;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    @Async("importProcessingExecutor")
    public void processAsync(UUID batchId) {
        try {
            processBatch(batchId);
        } catch (Exception e) {
            log.error("Batch processing failed for {}", batchId, e);
            markBatchFailed(batchId, e.getMessage());
        }
    }

    private void processBatch(UUID batchId) {
        ImportBatch batch = batchRepository.findById(batchId).orElse(null);
        if (batch == null) return;

        Map<String, String> mappings = parseJsonToStringMap(batch.getColumnMappings());
        int pageSize = 100;
        int pageNum = 0;
        int created = 0, duplicates = 0, rejected = 0, processed = 0;

        while (true) {
            Page<ImportRow> page = rowRepository.findByBatchIdOrderByRowNumberAsc(
                    batchId, PageRequest.of(pageNum, pageSize));

            if (page.isEmpty()) break;

            for (ImportRow row : page.getContent()) {
                try {
                    self.processRow(row, mappings, batch.getSource(), batch.getCreatedBy());
                    switch (row.getStatus()) {
                        case CREATED -> created++;
                        case DUPLICATE -> duplicates++;
                        case REJECTED -> rejected++;
                        default -> {}
                    }
                } catch (Exception e) {
                    row.setStatus(ImportRow.RowStatus.FAILED);
                    row.setErrorMessage(e.getMessage());
                    rowRepository.save(row);
                    rejected++;
                    log.warn("Row {} in batch {} failed: {}", row.getRowNumber(), batchId, e.getMessage());
                }
                processed++;
            }

            // update progress
            batch.setProcessedRows(processed);
            batch.setCreatedRows(created);
            batch.setDuplicateRows(duplicates);
            batch.setRejectedRows(rejected);
            batchRepository.save(batch);

            if (!page.hasNext()) break;
            pageNum++;
        }

        batch.setStatus(ImportBatch.BatchStatus.COMPLETED);
        batch.setProcessedRows(processed);
        batch.setCreatedRows(created);
        batch.setDuplicateRows(duplicates);
        batch.setRejectedRows(rejected);
        batchRepository.save(batch);

        log.info("Batch {} completed: {} processed, {} created, {} duplicates, {} rejected",
                batchId, processed, created, duplicates, rejected);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processRow(ImportRow row, Map<String, String> mappings, String source, String actor) {
        Map<String, Object> rawData = parseJsonToMap(row.getRawData());
        Map<String, String> mappedData = new LinkedHashMap<>();

        for (var entry : mappings.entrySet()) {
            Object value = rawData.get(entry.getKey());
            mappedData.put(entry.getValue(), value != null ? value.toString().trim() : "");
        }

        row.setMappedData(toJson(mappedData));

        String canonicalName = mappedData.getOrDefault("canonical_name", "").trim();
        if (canonicalName.isBlank()) {
            row.setStatus(ImportRow.RowStatus.REJECTED);
            row.setErrorMessage("Company name is required");
            rowRepository.save(row);
            return;
        }

        String normalizedName = nameNormalizer.normalize(canonicalName);
        String rawPhone = mappedData.getOrDefault("primary_phone_normalized", "");
        String normalizedPhone = phoneNormalizer.normalize(rawPhone);
        String rawWebsite = mappedData.getOrDefault("website_domain", "");
        String normalizedDomain = websiteNormalizer.normalize(rawWebsite);
        String city = mappedData.getOrDefault("city", "").trim();

        // duplicate detection: phone → domain → name+city
        Optional<Company> duplicate = Optional.empty();
        if (normalizedPhone != null && !normalizedPhone.isBlank() && phoneNormalizer.isValid(normalizedPhone)) {
            duplicate = companyRepository.findByPrimaryPhoneNormalized(normalizedPhone);
        }
        if (duplicate.isEmpty() && normalizedDomain != null && !normalizedDomain.isBlank()) {
            duplicate = companyRepository.findByWebsiteDomain(normalizedDomain);
        }
        if (duplicate.isEmpty() && !normalizedName.isBlank() && !city.isBlank()) {
            duplicate = companyRepository.findByNormalizedNameAndCity(normalizedName, city.toLowerCase());
        }

        if (duplicate.isPresent()) {
            row.setStatus(ImportRow.RowStatus.DUPLICATE);
            row.setDuplicateOfCompanyId(duplicate.get().getId());
            rowRepository.save(row);
            return;
        }

        // create company
        Company company = Company.builder()
                .canonicalName(canonicalName)
                .normalizedName(normalizedName)
                .primaryPhoneNormalized(normalizedPhone)
                .websiteDomain(normalizedDomain)
                .email(trimOrNull(mappedData.getOrDefault("email", "")))
                .city(trimOrNull(city))
                .state(trimOrNull(mappedData.getOrDefault("state", "")))
                .cluster(trimOrNull(mappedData.getOrDefault("cluster", "")))
                .industry(trimOrNull(mappedData.getOrDefault("industry", "")))
                .sizeBand(trimOrNull(mappedData.getOrDefault("size_band", "")))
                .source(source)
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        int completeness = computeCompleteness(company);
        company.setCompletenessScore(completeness);
        company = companyRepository.save(company);

        row.setStatus(ImportRow.RowStatus.CREATED);
        row.setCompanyId(company.getId());
        rowRepository.save(row);

        auditService.record("COMPANY", company.getId(), actor, "IMPORT_CREATE",
                null, companyMapper.toResponse(company));
    }

    private void markBatchFailed(UUID batchId, String error) {
        try {
            ImportBatch batch = batchRepository.findById(batchId).orElse(null);
            if (batch != null) {
                batch.setStatus(ImportBatch.BatchStatus.FAILED);
                batch.setErrorMessage(error);
                batchRepository.save(batch);
            }
        } catch (Exception e) {
            log.error("Failed to mark batch {} as failed", batchId, e);
        }
    }

    private int computeCompleteness(Company company) {
        int score = 0;
        int total = 8;
        if (company.getCanonicalName() != null && !company.getCanonicalName().isBlank()) score++;
        if (company.getPrimaryPhoneNormalized() != null && !company.getPrimaryPhoneNormalized().isBlank()) score++;
        if (company.getEmail() != null && !company.getEmail().isBlank()) score++;
        if (company.getWebsiteDomain() != null && !company.getWebsiteDomain().isBlank()) score++;
        if (company.getCity() != null && !company.getCity().isBlank()) score++;
        if (company.getState() != null && !company.getState().isBlank()) score++;
        if (company.getIndustry() != null && !company.getIndustry().isBlank()) score++;
        if (company.getSource() != null && !company.getSource().isBlank()) score++;
        return (int) Math.round((score * 100.0) / total);
    }

    private String trimOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
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
