package com.vyoog.prospectsoul_backend.imports.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.mapper.CompanyMapper;
import com.vyoog.prospectsoul_backend.company.nic.entity.CompanyNicCode;
import com.vyoog.prospectsoul_backend.company.nic.repository.CompanyNicCodeRepository;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.imports.entity.ImportBatch;
import com.vyoog.prospectsoul_backend.imports.entity.ImportRow;
import com.vyoog.prospectsoul_backend.imports.normalization.CompanyNameNormalizer;
import com.vyoog.prospectsoul_backend.imports.normalization.PhoneNormalizer;
import com.vyoog.prospectsoul_backend.imports.normalization.WebsiteNormalizer;
import com.vyoog.prospectsoul_backend.imports.parser.ActivitiesJsonParser;
import com.vyoog.prospectsoul_backend.imports.repository.ImportBatchRepository;
import com.vyoog.prospectsoul_backend.imports.repository.ImportRowRepository;
import com.vyoog.prospectsoul_backend.nic.entity.NicCode;
import com.vyoog.prospectsoul_backend.nic.repository.NicCodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * High-throughput import processor.
 *
 * The prior implementation ran 6–10 DB round-trips per row (four dedup
 * SELECTs, one nic-code SELECT per activity, per-row audit INSERT) plus a
 * frequent batch counter UPDATE. On a 100k-row registry file this was
 * ~30 minutes.
 *
 * This rewrite loads a compact dedup snapshot into memory once at batch
 * start (phones, domains, source-refs, name+city / name+pincode keys),
 * caches the whole {@code nic_codes} master by {@code code}, and moves
 * per-row audit into a single batch-level audit at completion. Row
 * inserts and join-row inserts flush via {@code saveAll} in chunks of
 * {@link #CHUNK}. The result is 1–2 DB round-trips per company on
 * average, and the entire batch runs in one @Async task while the
 * executor pool stays free for parallel batches.
 */
@Service
@Slf4j
public class ImportProcessingService {

    private static final int CHUNK = 200;

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
    private final JdbcTemplate jdbcTemplate;
    private final ImportProcessingService self;

    @PersistenceContext
    private EntityManager entityManager;

    private static final DateTimeFormatter[] REG_DATE_FORMATS = new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE
    };

    public ImportProcessingService(
            ImportBatchRepository batchRepository, ImportRowRepository rowRepository,
            CompanyRepository companyRepository, CompanyMapper companyMapper,
            AuditService auditService, CompanyNameNormalizer nameNormalizer,
            PhoneNormalizer phoneNormalizer, WebsiteNormalizer websiteNormalizer,
            ObjectMapper objectMapper, ActivitiesJsonParser activitiesParser,
            NicCodeRepository nicCodeRepository, CompanyNicCodeRepository companyNicCodeRepository,
            JdbcTemplate jdbcTemplate, @Lazy ImportProcessingService self) {
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
        this.jdbcTemplate = jdbcTemplate;
        this.self = self;
    }

    @Async("importProcessingExecutor")
    public void processAsync(UUID batchId) {
        try {
            self.processBatch(batchId);
        } catch (Exception e) {
            log.error("Batch processing failed for {}", batchId, e);
            markBatchFailed(batchId, e.getMessage());
        }
    }

    /**
     * Entry point for tests and callers that want a synchronous run.
     *
     * NOT annotated @Transactional. The method itself only orchestrates —
     * each chunk is written inside its own REQUIRES_NEW transaction (see
     * {@link #processChunk}) so parent-row FK locks are released between
     * chunks and the progress commit helper never deadlocks with the
     * chunk transaction.
     */
    public void processBatch(UUID batchId) {
        ImportBatch batch = batchRepository.findById(batchId).orElse(null);
        if (batch == null) return;

        long t0 = System.currentTimeMillis();
        Map<String, String> mappings = parseJsonToStringMap(batch.getColumnMappings());

        // --- Load dedup snapshot ONCE. All lookups are then in-memory. ---
        DedupContext ctx = loadDedupContext(batch.getSource());
        log.info("import batch {} dedup snapshot: {} phones, {} domains, {} src-refs, {} name+city, {} name+pin, nic-codes={} — loaded in {} ms",
                batchId, ctx.phones.size(), ctx.domains.size(), ctx.sourceRefs.size(),
                ctx.nameCity.size(), ctx.namePincode.size(), ctx.nicByCode.size(),
                System.currentTimeMillis() - t0);

        // Flip to PROCESSING immediately so the UI shows liveness.
        // Use jdbcTemplate.update so the outer @Transactional never holds
        // a row-level lock on the batch row — that would deadlock the
        // REQUIRES_NEW commitBatchProgress helper below.
        if (batch.getStatus() != ImportBatch.BatchStatus.PROCESSING) {
            jdbcTemplate.update(
                    "UPDATE import_batches SET status = ?, updated_at = now() WHERE id = ?",
                    ImportBatch.BatchStatus.PROCESSING.name(), batchId);
        }
        // Detach the batch entity so Hibernate never tries to auto-flush
        // stale changes back into the row.
        entityManager.detach(batch);

        AtomicInteger created = new AtomicInteger();
        AtomicInteger duplicates = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        AtomicInteger processed = new AtomicInteger();
        AtomicLong lastFlushMs = new AtomicLong();

        int pageNum = 0;
        while (true) {
            Page<ImportRow> page = rowRepository.findByBatchIdOrderByRowNumberAsc(
                    batchId, PageRequest.of(pageNum, CHUNK));
            if (page.isEmpty()) break;

            List<ImportRow> rowsToSave = new ArrayList<>(page.getContent().size());
            List<Pending> pending = new ArrayList<>(page.getContent().size());

            for (ImportRow row : page.getContent()) {
                try {
                    processOne(row, mappings, batch.getSource(), batch.getCreatedBy(),
                               ctx, pending);
                    switch (row.getStatus()) {
                        case CREATED   -> created.incrementAndGet();
                        case DUPLICATE -> duplicates.incrementAndGet();
                        case REJECTED  -> rejected.incrementAndGet();
                        default -> {}
                    }
                } catch (Exception e) {
                    // Skip-on-error — one bad row never stops the batch.
                    row.setStatus(ImportRow.RowStatus.FAILED);
                    row.setErrorMessage(truncate(e.getMessage(), 4000));
                    row.setOutcomeReason("exception");
                    rejected.incrementAndGet();
                    log.warn("Row {} in batch {} failed: {}", row.getRowNumber(), batchId, e.getMessage());
                }
                rowsToSave.add(row);
                processed.incrementAndGet();
            }

            // Persist companies + joins + row-status update inside ONE
            // REQUIRES_NEW transaction per chunk. Any RuntimeException here
            // triggers the per-row fallback (also its own transaction).
            try {
                self.processChunk(pending, rowsToSave, ctx);
            } catch (RuntimeException e) {
                log.warn("bulk chunk persist failed ({}); falling back to per-row for {} row(s)",
                        e.toString(), pending.size());
                for (Pending p : pending) {
                    try {
                        self.persistOne(p, ctx);
                    } catch (RuntimeException oneErr) {
                        p.row.setStatus(ImportRow.RowStatus.FAILED);
                        p.row.setErrorMessage(truncate(rootCauseMessage(oneErr), 4000));
                        p.row.setOutcomeReason("persist_failed");
                        created.decrementAndGet();
                        rejected.incrementAndGet();
                    }
                }
                // Even if the bulk chunk rolled back, the fallback wrote each
                // row's company. The row-status rows still need updating.
                self.commitRowStatuses(rowsToSave);
            }

            long now = System.currentTimeMillis();
            if (now - lastFlushMs.get() >= 700) {
                // Commit progress in its own transaction so the polling
                // frontend can actually see the counters advance. If we
                // used saveAndFlush inside the outer @Transactional, the
                // UPDATE would sit in the transaction until the whole
                // batch finished — the user would just see 0/N for the
                // entire run, then jump to N/N at the end.
                self.commitBatchProgress(batchId, processed.get(), created.get(),
                        duplicates.get(), rejected.get());
                lastFlushMs.set(now);
            }

            if (!page.hasNext()) break;
            pageNum++;
        }

        jdbcTemplate.update(
                "UPDATE import_batches SET status = ?, processed_rows = ?, created_rows = ?, " +
                "duplicate_rows = ?, rejected_rows = ?, updated_at = now() WHERE id = ?",
                ImportBatch.BatchStatus.COMPLETED.name(),
                processed.get(), created.get(), duplicates.get(), rejected.get(), batchId);

        // Batch-level audit — one row per completed import instead of one
        // row per created company. The per-row lineage lives in import_rows
        // and audit_log queries against COMPANY still find the record when
        // needed via the timeline join.
        // AuditService.record is @Transactional(MANDATORY), and processBatch
        // is deliberately non-transactional (see the deadlock note above), so
        // the call must run inside its own REQUIRES_NEW transaction. Without
        // this, the completion audit throws IllegalTransactionStateException
        // and processAsync flips the just-set COMPLETED status back to FAILED.
        self.recordBatchCompletion(batch.getId(), batch.getCreatedBy(), batch.getSource(),
                batch.getFileName(), processed.get(), created.get(), duplicates.get(),
                rejected.get(), System.currentTimeMillis() - t0);

        log.info("Batch {} completed in {} ms: {} processed, {} created, {} duplicates, {} rejected",
                batchId, System.currentTimeMillis() - t0, processed.get(), created.get(),
                duplicates.get(), rejected.get());
    }

    /** One row's worth of work — mutates {@code row} and appends to the buffers. */
    private void processOne(ImportRow row, Map<String, String> mappings, String source,
                             String actor, DedupContext ctx,
                             List<Pending> pending) {
        Map<String, Object> rawData = parseJsonToMap(row.getRawData());
        Map<String, String> mappedData = new LinkedHashMap<>();
        for (var entry : mappings.entrySet()) {
            Object v = rawData.get(entry.getKey());
            mappedData.put(entry.getValue(), v != null ? v.toString().trim() : "");
        }
        row.setMappedData(toJson(mappedData));

        String canonicalName = mappedData.getOrDefault("canonical_name", "").trim();
        if (canonicalName.isBlank()) {
            row.setStatus(ImportRow.RowStatus.REJECTED);
            row.setErrorMessage("Company name is required");
            row.setOutcomeReason("missing_name");
            return;
        }

        String normalizedName  = nameNormalizer.normalize(canonicalName);
        String normalizedPhone = phoneNormalizer.normalize(mappedData.getOrDefault("primary_phone_normalized", ""));
        String normalizedDomain= websiteNormalizer.normalize(mappedData.getOrDefault("website_domain", ""));
        String city            = mappedData.getOrDefault("city", "").trim();
        String pincode         = digitsOnly(mappedData.getOrDefault("pincode", ""), 6);
        String sourceReference = trimOrNull(mappedData.getOrDefault("source_reference", ""));
        if (sourceReference == null && "UDYAM_MSME_REGISTRY".equals(source)) {
            sourceReference = buildUdyamSourceReference(mappedData, normalizedName);
        }

        // In-memory dedup — same rule order as before (ADR-0004).
        UUID matchedId = null; String matchReason = null;
        if (sourceReference != null) {
            matchedId = ctx.sourceRefs.get(source + "|" + sourceReference);
            if (matchedId != null) matchReason = "source_reference_match";
        }
        if (matchedId == null && normalizedPhone != null && !normalizedPhone.isBlank()
                && phoneNormalizer.isValid(normalizedPhone)) {
            matchedId = ctx.phones.get(normalizedPhone);
            if (matchedId != null) matchReason = "phone_match";
        }
        if (matchedId == null && normalizedDomain != null && !normalizedDomain.isBlank()) {
            matchedId = ctx.domains.get(normalizedDomain);
            if (matchedId != null) matchReason = "domain_match";
        }
        if (matchedId == null && !normalizedName.isBlank()) {
            boolean isRegistry = source != null && source.endsWith("REGISTRY");
            if (isRegistry && pincode != null) {
                matchedId = ctx.namePincode.get(normalizedName + "|" + pincode);
                if (matchedId != null) matchReason = "name_pincode_match";
            } else if (!isRegistry && !city.isBlank()) {
                matchedId = ctx.nameCity.get(normalizedName + "|" + city.toLowerCase(Locale.ROOT));
                if (matchedId != null) matchReason = "name_city_match";
            }
        }

        if (matchedId != null) {
            row.setStatus(ImportRow.RowStatus.DUPLICATE);
            row.setDuplicateOfCompanyId(matchedId);
            row.setOutcomeReason(matchReason);
            return;
        }

        // Build & buffer the company for a bulk insert.
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
                .updatedBy(actor)
                .build();
        company.setCompletenessScore(computeCompleteness(company));
        // Deferred dedup keys — filled in after JPA assigns the company id.
        List<DedupKey> dedupKeys = new ArrayList<>(4);
        if (normalizedPhone != null && !normalizedPhone.isBlank())
            dedupKeys.add((id, c) -> c.phones.putIfAbsent(normalizedPhone, id));
        final String domainKey = normalizedDomain;
        if (domainKey != null && !domainKey.isBlank())
            dedupKeys.add((id, c) -> c.domains.putIfAbsent(domainKey, id));
        final String srcRef = sourceReference;
        if (srcRef != null)
            dedupKeys.add((id, c) -> c.sourceRefs.putIfAbsent(source + "|" + srcRef, id));
        if (!normalizedName.isBlank()) {
            if (pincode != null)
                dedupKeys.add((id, c) -> c.namePincode.putIfAbsent(normalizedName + "|" + pincode, id));
            final String cityKey = city;
            if (!cityKey.isBlank())
                dedupKeys.add((id, c) -> c.nameCity.putIfAbsent(normalizedName + "|" + cityKey.toLowerCase(Locale.ROOT), id));
        }

        // Parse activities but keep them as tuples — actual persistence
        // happens after the parent company gets its ID.
        String rowOutcome = null;
        List<Object[]> activityTuples = new ArrayList<>();
        String activitiesJson = mappedData.get("activities_json");
        ActivitiesJsonParser.Result actResult = activitiesParser.parse(activitiesJson);
        if (actResult instanceof ActivitiesJsonParser.Result.Ok ok) {
            // Some registry rows repeat the same NIC code twice on one
            // company (e.g. Kanchipuram has 22199 duplicated on ~55 rows).
            // The DB uniqueness constraint uq_cnc_company_raw (company_id,
            // nic_code_raw) rejects the second insert and — because we
            // batch — the entire chunk rolls back. Deduplicate here,
            // keeping the first occurrence's sequence and description.
            java.util.Set<String> seen = new java.util.HashSet<>();
            short seq = 1;
            for (ActivitiesJsonParser.Activity a : ok.activities()) {
                String code = a.nicCode();
                if (code == null || !seen.add(code)) continue;
                NicCode resolved = ctx.nicByCode.get(code);
                activityTuples.add(new Object[] { resolved, code, a.description(), seq });
                seq++;
            }
        } else if (actResult instanceof ActivitiesJsonParser.Result.Invalid inv) {
            rowOutcome = "activities_json_invalid";
            row.setErrorMessage("activities_json_invalid: " + inv.reason());
        }

        row.setStatus(ImportRow.RowStatus.CREATED);
        row.setOutcomeReason(rowOutcome != null ? rowOutcome : "created");

        pending.add(new Pending(row, company, activityTuples, dedupKeys));
    }

    /** Post-persist hook that folds a fresh company id into a dedup map. */
    @FunctionalInterface
    private interface DedupKey {
        void assign(UUID id, DedupContext ctx);
    }

    /** Row + company + parsed activities + deferred dedup key updates. */
    private static final class Pending {
        final ImportRow row;
        final Company company;
        final List<Object[]> activities;
        final List<DedupKey> dedupKeys;
        Pending(ImportRow row, Company company, List<Object[]> activities, List<DedupKey> dedupKeys) {
            this.row = row; this.company = company; this.activities = activities; this.dedupKeys = dedupKeys;
        }
    }

    // -------- Dedup snapshot loader --------

    /**
     * Loads compact in-memory indexes of every existing company. All
     * lookups during the import are O(1) hashmap hits instead of DB
     * round-trips. Loaded via a single native SELECT so it takes only a
     * few seconds even at ~200k companies.
     */
    private DedupContext loadDedupContext(String source) {
        DedupContext ctx = new DedupContext();

        jdbcTemplate.query("""
                SELECT id, primary_phone_normalized, website_domain, source, source_reference,
                       normalized_name, city, pincode
                  FROM companies
                """, (rs) -> {
            UUID id = (UUID) rs.getObject("id");
            String phone   = rs.getString("primary_phone_normalized");
            String domain  = rs.getString("website_domain");
            String src     = rs.getString("source");
            String srcRef  = rs.getString("source_reference");
            String name    = rs.getString("normalized_name");
            String city    = rs.getString("city");
            String pin     = rs.getString("pincode");
            if (phone != null && !phone.isBlank())   ctx.phones.putIfAbsent(phone, id);
            if (domain != null && !domain.isBlank()) ctx.domains.putIfAbsent(domain, id);
            if (src != null && srcRef != null && !srcRef.isBlank())
                ctx.sourceRefs.putIfAbsent(src + "|" + srcRef, id);
            if (name != null && !name.isBlank()) {
                if (pin != null && !pin.isBlank())    ctx.namePincode.putIfAbsent(name + "|" + pin, id);
                if (city != null && !city.isBlank())  ctx.nameCity.putIfAbsent(name + "|" + city.toLowerCase(Locale.ROOT), id);
            }
        });

        // Cache the whole NIC master by code — usually ~2000 rows.
        for (NicCode c : nicCodeRepository.findAll()) {
            ctx.nicByCode.put(c.getCode(), c);
        }
        return ctx;
    }

    /**
     * Backwards-compatible entry point for the older test suite that
     * called {@code processRow} directly. Kept as a thin delegator that
     * inserts one row without the batch dedup snapshot.
     */
    @Transactional
    public void processRow(ImportRow row, Map<String, String> mappings, String source, String actor) {
        DedupContext ctx = loadDedupContext(source);
        List<Pending> pending = new ArrayList<>(1);
        processOne(row, mappings, source, actor, ctx, pending);
        for (Pending p : pending) {
            entityManager.persist(p.company);
        }
        entityManager.flush();
        for (Pending p : pending) {
            UUID cid = p.company.getId();
            p.row.setCompanyId(cid);
            for (Object[] act : p.activities) {
                NicCode resolved = (NicCode) act[0];
                short seq = (short) act[3];
                entityManager.persist(CompanyNicCode.builder()
                        .companyId(cid).nicCode(resolved).nicCodeRaw((String) act[1])
                        .descriptionRaw((String) act[2]).isPrimary(seq == 1).sequenceNo(seq).build());
                if (seq == 1 && resolved != null) p.company.setPrimaryNicCodeId(resolved.getId());
            }
        }
        entityManager.flush();
        rowRepository.save(row);
    }

    private void markBatchFailed(UUID batchId, String error) {
        try {
            ImportBatch batch = batchRepository.findById(batchId).orElse(null);
            if (batch != null) {
                // Never regress a terminal status. If the batch already
                // reached COMPLETED (or was previously marked FAILED and
                // reprocessed), a late exception in a post-completion step
                // must not roll the visible status back.
                ImportBatch.BatchStatus s = batch.getStatus();
                if (s == ImportBatch.BatchStatus.COMPLETED || s == ImportBatch.BatchStatus.FAILED) {
                    log.warn("Batch {} already in terminal status {} — not overwriting with FAILED ({})",
                            batchId, s, error);
                    return;
                }
                batch.setStatus(ImportBatch.BatchStatus.FAILED);
                batch.setErrorMessage(error);
                batchRepository.save(batch);
            }
        } catch (Exception e) {
            log.error("Failed to mark batch {} as failed", batchId, e);
        }
    }

    // -------- Helpers --------

    private String buildUdyamSourceReference(Map<String, String> m, String normalizedName) {
        String st = nz(m.get("lg_state_code"));
        String dt = nz(m.get("lg_district_code"));
        String pin = digitsOnly(nz(m.get("pincode")), 6);
        String reg = nz(m.get("registration_date"));
        String hash = Integer.toHexString((normalizedName + "|" + reg).hashCode());
        return String.join("-", st, dt, pin == null ? "" : pin, hash);
    }

    private String nz(String v) { return v == null ? "" : v.trim(); }

    private String digitsOnly(String raw, int exact) {
        if (raw == null) return null;
        String d = raw.replaceAll("[^0-9]", "");
        if (d.isEmpty()) return null;
        if (exact > 0 && d.length() != exact) return null;
        return d;
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

    private String trimOrNull(String v) {
        if (v == null || v.isBlank()) return null;
        return v.trim();
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (JacksonException e) { return "{}"; }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonToMap(String json) {
        try { return objectMapper.readValue(json, Map.class); }
        catch (JacksonException e) { return Map.of(); }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseJsonToStringMap(String json) {
        try { return objectMapper.readValue(json, Map.class); }
        catch (JacksonException e) { return Map.of(); }
    }

    /** Bag of in-memory dedup indexes, populated once per batch. */
    private static final class DedupContext {
        final Map<String, UUID> phones     = new ConcurrentHashMap<>(16384);
        final Map<String, UUID> domains    = new ConcurrentHashMap<>(4096);
        final Map<String, UUID> sourceRefs = new ConcurrentHashMap<>(65536);
        final Map<String, UUID> nameCity   = new ConcurrentHashMap<>(32768);
        final Map<String, UUID> namePincode= new ConcurrentHashMap<>(32768);
        final Map<String, NicCode> nicByCode = new HashMap<>(4096);
    }

    /** Fast bulk path — persists all companies then all join rows via JPA batch inserts. */
    private void persistChunkBulk(List<Pending> pending, DedupContext ctx) {
        for (Pending p : pending) {
            entityManager.persist(p.company);
        }
        entityManager.flush();
        for (Pending p : pending) {
            UUID companyId = p.company.getId();
            p.row.setCompanyId(companyId);
            for (Object[] act : p.activities) {
                NicCode resolved = (NicCode) act[0];
                String rawCode = (String) act[1];
                String rawDesc = (String) act[2];
                short seq      = (short) act[3];
                entityManager.persist(CompanyNicCode.builder()
                        .companyId(companyId)
                        .nicCode(resolved)
                        .nicCodeRaw(rawCode)
                        .descriptionRaw(rawDesc)
                        .isPrimary(seq == 1)
                        .sequenceNo(seq)
                        .build());
                if (seq == 1 && resolved != null) {
                    p.company.setPrimaryNicCodeId(resolved.getId());
                }
            }
            if (p.dedupKeys != null) for (DedupKey k : p.dedupKeys) k.assign(companyId, ctx);
        }
        entityManager.flush();
        // Do not clear the persistence context here — subsequent per-chunk
        // work uses jdbcTemplate for updates, not JPA merge, so keeping the
        // context small is not critical and clearing detaches ImportRow
        // entities which then trigger a costly merge path.
    }

    /**
     * Per-row fallback used when the bulk path throws. Each row gets its
     * own transaction so a constraint violation on one row leaves every
     * other row committed. Called via {@code self} so the proxy applies
     * REQUIRES_NEW.
     */
    @org.springframework.transaction.annotation.Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void persistOne(Pending p, DedupContext ctx) {
        entityManager.persist(p.company);
        entityManager.flush();
        UUID companyId = p.company.getId();
        p.row.setCompanyId(companyId);
        for (Object[] act : p.activities) {
            NicCode resolved = (NicCode) act[0];
            String rawCode = (String) act[1];
            String rawDesc = (String) act[2];
            short seq      = (short) act[3];
            entityManager.persist(CompanyNicCode.builder()
                    .companyId(companyId).nicCode(resolved).nicCodeRaw(rawCode)
                    .descriptionRaw(rawDesc).isPrimary(seq == 1).sequenceNo(seq).build());
            if (seq == 1 && resolved != null) p.company.setPrimaryNicCodeId(resolved.getId());
        }
        entityManager.flush();
        if (p.dedupKeys != null) for (DedupKey k : p.dedupKeys) k.assign(companyId, ctx);
    }

    private static String rootCauseMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) c = c.getCause();
        return c.getMessage() == null ? t.toString() : c.getMessage();
    }


    /**
     * One JDBC batch per chunk. Writes the six mutable columns on
     * import_rows for the whole chunk. Bypasses JPA merge so we skip
     * 200 SELECT+UPDATE round-trips per chunk.
     */
    private void bulkUpdateRows(List<ImportRow> rows) {
        if (rows.isEmpty()) return;
        jdbcTemplate.batchUpdate(
                "UPDATE import_rows SET status = ?, company_id = ?, duplicate_of_company_id = ?, " +
                "error_message = ?, mapped_data = ?::jsonb, outcome_reason = ?, updated_at = now() " +
                "WHERE id = ?",
                rows,
                rows.size(),
                (ps, r) -> {
                    ps.setString(1, r.getStatus().name());
                    ps.setObject(2, r.getCompanyId());
                    ps.setObject(3, r.getDuplicateOfCompanyId());
                    ps.setString(4, r.getErrorMessage());
                    ps.setString(5, r.getMappedData());
                    ps.setString(6, r.getOutcomeReason());
                    ps.setObject(7, r.getId());
                }
        );
    }


    /**
     * Writes the four batch counters in an independent transaction so
     * the polling UI can see progress advance while the outer batch
     * processing is still running.
     */
    @org.springframework.transaction.annotation.Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void commitBatchProgress(UUID batchId, int processed, int created, int duplicates, int rejected) {
        jdbcTemplate.update(
                "UPDATE import_batches SET processed_rows = ?, created_rows = ?, " +
                "duplicate_rows = ?, rejected_rows = ?, updated_at = now() WHERE id = ?",
                processed, created, duplicates, rejected, batchId);
    }


    /**
     * One chunk of work in its own transaction: persist companies +
     * their join rows, then bulk-update the row-status table. Runs in
     * REQUIRES_NEW so the loop in processBatch never holds a
     * long-lived transaction (which would keep FK share-locks on the
     * parent import_batches row and deadlock commitBatchProgress).
     */
    @org.springframework.transaction.annotation.Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void processChunk(List<Pending> pending, List<ImportRow> rowsToSave, DedupContext ctx) {
        persistChunkBulk(pending, ctx);
        bulkUpdateRows(rowsToSave);
    }

    /**
     * Standalone helper — just the row-status batch update in its own
     * transaction. Called after the per-row fallback so the row status
     * table catches up even when the fast path aborted.
     */
    @org.springframework.transaction.annotation.Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void commitRowStatuses(List<ImportRow> rows) {
        bulkUpdateRows(rows);
    }

    /**
     * Completion audit in its own transaction so AuditService's MANDATORY
     * propagation is satisfied without making the outer processBatch loop
     * transactional (which would reintroduce the FK-lock deadlock with
     * commitBatchProgress).
     */
    @org.springframework.transaction.annotation.Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void recordBatchCompletion(UUID batchId, String actor, String source, String fileName,
                                       int processed, int created, int duplicates, int rejected,
                                       long durationMs) {
        auditService.record("IMPORT_BATCH", batchId, actor, "IMPORT_COMPLETE", null, Map.of(
                "batch_id", batchId.toString(),
                "source", source,
                "file", fileName,
                "created", created,
                "duplicates", duplicates,
                "rejected", rejected,
                "processed", processed,
                "duration_ms", durationMs));
    }

}
