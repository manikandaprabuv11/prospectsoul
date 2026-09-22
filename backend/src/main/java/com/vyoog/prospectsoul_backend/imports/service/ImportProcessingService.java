package com.vyoog.prospectsoul_backend.imports.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.company.nic.entity.CompanyNicCode;
import com.vyoog.prospectsoul_backend.company.nic.repository.CompanyNicCodeRepository;
import com.vyoog.prospectsoul_backend.imports.parser.ActivitiesJsonParser;
import com.vyoog.prospectsoul_backend.nic.entity.NicCode;
import com.vyoog.prospectsoul_backend.nic.repository.NicCodeRepository;
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
    private final ActivitiesJsonParser activitiesParser;
    private final NicCodeRepository nicCodeRepository;
    private final CompanyNicCodeRepository companyNicCodeRepository;
    private final ImportProcessingService self;

    private static final DateTimeFormatter[] REG_DATE_FORMATS = new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE
    };

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
            ActivitiesJsonParser activitiesParser,
            NicCodeRepository nicCodeRepository,
            CompanyNicCodeRepository companyNicCodeRepository,
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
        this.activitiesParser = activitiesParser;
        this.nicCodeRepository = nicCodeRepository;
        this.companyNicCodeRepository = companyNicCodeRepository;
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

        // Ensure PROCESSING status is visible before the first row lands
        // (the controller already flips to PROCESSING on start, but this
        // is defensive in case the caller invoked us directly).
        if (batch.getStatus() != ImportBatch.BatchStatus.PROCESSING) {
            batch.setStatus(ImportBatch.BatchStatus.PROCESSING);
            batchRepository.save(batch);
        }

        long lastFlushMs = 0;
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
                    // Skip-on-error — one bad row never stops the batch.
                    // The per-row REQUIRES_NEW transaction already rolled
                    // back this row's writes; mark it FAILED so the user
                    // can see exactly which row and why.
                    try {
                        row.setStatus(ImportRow.RowStatus.FAILED);
                        row.setErrorMessage(truncate(e.getMessage(), 4000));
                        row.setOutcomeReason("exception");
                        rowRepository.save(row);
                    } catch (Exception saveEx) {
                        log.warn("could not mark row {} as FAILED: {}",
                                row.getRowNumber(), saveEx.toString());
                    }
                    rejected++;
                    log.warn("Row {} in batch {} failed: {}", row.getRowNumber(), batchId, e.getMessage());
                }
                processed++;

                // Push progress every row for the first 20 (so the user
                // sees liveness immediately on small files), else at
                // most every 500 ms. Uses a single UPDATE per flush.
                long nowMs = System.currentTimeMillis();
                if (processed <= 20 || nowMs - lastFlushMs >= 500) {
                    batch.setProcessedRows(processed);
                    batch.setCreatedRows(created);
                    batch.setDuplicateRows(duplicates);
                    batch.setRejectedRows(rejected);
                    batchRepository.saveAndFlush(batch);
                    lastFlushMs = nowMs;
                }
            }

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
            row.setOutcomeReason("missing_name");
            rowRepository.save(row);
            return;
        }

        String normalizedName = nameNormalizer.normalize(canonicalName);
        String rawPhone = mappedData.getOrDefault("primary_phone_normalized", "");
        String normalizedPhone = phoneNormalizer.normalize(rawPhone);
        String rawWebsite = mappedData.getOrDefault("website_domain", "");
        String normalizedDomain = websiteNormalizer.normalize(rawWebsite);
        String city = mappedData.getOrDefault("city", "").trim();
        String pincode = digitsOnly(mappedData.getOrDefault("pincode", ""), 6);
        String sourceReference = trimOrNull(mappedData.getOrDefault("source_reference", ""));

        // For registry sources the source_reference is derived from the source
        // row itself when the file does not supply it explicitly (ADR-0004).
        if (sourceReference == null && "UDYAM_MSME_REGISTRY".equals(source)) {
            sourceReference = buildUdyamSourceReference(mappedData, normalizedName);
        }

        // Dedup order (ADR-0004):
        //   ⓪ source + source_reference — first, catches registry re-imports
        //   ① normalized phone
        //   ② website domain
        //   ③ normalized name + city  (loose sources)
        //   ③′ normalized name + pincode  (registry sources — same-district files)
        Optional<Company> duplicate = Optional.empty();
        String outcome = null;
        if (sourceReference != null) {
            duplicate = companyRepository.findBySourceAndSourceReference(source, sourceReference);
            if (duplicate.isPresent()) outcome = "source_reference_match";
        }
        if (duplicate.isEmpty() && normalizedPhone != null && !normalizedPhone.isBlank()
                && phoneNormalizer.isValid(normalizedPhone)) {
            duplicate = companyRepository.findByPrimaryPhoneNormalized(normalizedPhone);
            if (duplicate.isPresent()) outcome = "phone_match";
        }
        if (duplicate.isEmpty() && normalizedDomain != null && !normalizedDomain.isBlank()) {
            duplicate = companyRepository.findByWebsiteDomain(normalizedDomain);
            if (duplicate.isPresent()) outcome = "domain_match";
        }
        if (duplicate.isEmpty() && !normalizedName.isBlank()) {
            boolean isRegistry = source != null && source.endsWith("REGISTRY");
            if (isRegistry && pincode != null) {
                duplicate = companyRepository.findByNormalizedNameAndPincode(normalizedName, pincode);
                if (duplicate.isPresent()) outcome = "name_pincode_match";
            } else if (!isRegistry && !city.isBlank()) {
                duplicate = companyRepository.findByNormalizedNameAndCity(normalizedName, city.toLowerCase());
                if (duplicate.isPresent()) outcome = "name_city_match";
            }
        }

        if (duplicate.isPresent()) {
            row.setStatus(ImportRow.RowStatus.DUPLICATE);
            row.setDuplicateOfCompanyId(duplicate.get().getId());
            row.setOutcomeReason(outcome);
            rowRepository.save(row);
            return;
        }

        // Build & save the company with the Sales-Intelligence field set.
        Company.CompanyBuilder builder = Company.builder()
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
                .pincode(pincode)
                .district(trimOrNull(mappedData.getOrDefault("district", "")))
                .addressLine(trimOrNull(mappedData.getOrDefault("address_line", "")))
                .region(trimOrNull(mappedData.getOrDefault("region", "")))
                .products(trimOrNull(mappedData.getOrDefault("products", "")))
                .turnover(parseBigDecimal(mappedData.getOrDefault("turnover", "")))
                .gstNumber(trimOrNull(mappedData.getOrDefault("gst_number", "")))
                .employeeCount(parseInt(mappedData.getOrDefault("employee_count", "")))
                .registrationDate(parseRegDate(mappedData.getOrDefault("registration_date", "")))
                .sourceReference(sourceReference)
                .lgStateCode(parseShort(mappedData.getOrDefault("lg_state_code", "")))
                .lgDistrictCode(parseInt(mappedData.getOrDefault("lg_district_code", "")))
                .createdBy(actor)
                .updatedBy(actor);

        Company company = builder.build();
        company.setCompletenessScore(computeCompleteness(company));
        company = companyRepository.save(company);

        // Multi-NIC join rows from Activities JSON (Domain Model Addendum
        // §8.1). Malformed JSON does NOT fail the row.
        String activitiesJson = mappedData.get("activities_json");
        ActivitiesJsonParser.Result actResult = activitiesParser.parse(activitiesJson);
        String rowOutcome = null;
        if (actResult instanceof ActivitiesJsonParser.Result.Ok ok) {
            short seq = 1;
            for (ActivitiesJsonParser.Activity a : ok.activities()) {
                NicCode resolved = nicCodeRepository.findByCode(a.nicCode()).orElse(null);
                CompanyNicCode row2 = CompanyNicCode.builder()
                        .companyId(company.getId())
                        .nicCode(resolved)
                        .nicCodeRaw(a.nicCode())
                        .descriptionRaw(a.description())
                        .isPrimary(seq == 1)
                        .sequenceNo(seq)
                        .build();
                companyNicCodeRepository.save(row2);
                if (seq == 1 && resolved != null) {
                    company.setPrimaryNicCodeId(resolved.getId());
                    companyRepository.save(company);
                }
                seq++;
            }
        } else if (actResult instanceof ActivitiesJsonParser.Result.Invalid inv) {
            rowOutcome = "activities_json_invalid";
            row.setErrorMessage("activities_json_invalid: " + inv.reason());
        }

        row.setStatus(ImportRow.RowStatus.CREATED);
        row.setCompanyId(company.getId());
        row.setOutcomeReason(rowOutcome != null ? rowOutcome : "created");
        rowRepository.save(row);

        auditService.record("COMPANY", company.getId(), actor, "IMPORT_CREATE",
                null, companyMapper.toResponse(company));
    }

    private String buildUdyamSourceReference(Map<String, String> mapped, String normalizedName) {
        String st = mapped.getOrDefault("lg_state_code", "");
        String dt = mapped.getOrDefault("lg_district_code", "");
        String pin = digitsOnly(mapped.getOrDefault("pincode", ""), 6);
        String reg = mapped.getOrDefault("registration_date", "");
        String hash = Integer.toHexString((normalizedName + "|" + reg).hashCode());
        return String.join("-", nz(st), nz(dt), nz(pin), hash);
    }

    private String nz(String v) { return v == null ? "" : v.trim(); }

    private String digitsOnly(String raw, int exact) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;
        if (exact > 0 && digits.length() != exact) return null;
        return digits;
    }

    private BigDecimal parseBigDecimal(String v) {
        if (v == null || v.isBlank()) return null;
        try { return new BigDecimal(v.replaceAll("[,\\s]", "")); }
        catch (NumberFormatException e) { return null; }
    }

    private Integer parseInt(String v) {
        if (v == null || v.isBlank()) return null;
        try { return Integer.parseInt(v.replaceAll("[,\\s]", "").trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private Short parseShort(String v) {
        if (v == null || v.isBlank()) return null;
        try { return Short.parseShort(v.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private LocalDate parseRegDate(String v) {
        if (v == null || v.isBlank()) return null;
        for (DateTimeFormatter f : REG_DATE_FORMATS) {
            try { return LocalDate.parse(v.trim(), f); }
            catch (Exception ignored) {}
        }
        return null;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "…";
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
