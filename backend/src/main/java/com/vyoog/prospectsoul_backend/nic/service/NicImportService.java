package com.vyoog.prospectsoul_backend.nic.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.common.exception.BusinessRuleException;
import com.vyoog.prospectsoul_backend.nic.dto.response.NicImportResultResponse;
import com.vyoog.prospectsoul_backend.nic.entity.NicCode;
import com.vyoog.prospectsoul_backend.nic.parser.NicLevelResolver;
import com.vyoog.prospectsoul_backend.nic.repository.NicCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * NIC master reference-data import (Kickoff constraint 6). Deliberately does
 * NOT create import_batches / import_rows — those model prospect lineage,
 * and a classification list has no lineage story.
 *
 * The parent resolution algorithm is the one specified in docs 21 §3.3:
 *   1. Determine level = length(code).
 *   2. Sort by level ascending — parents always land before children.
 *   3. For each row, resolve parent as the longest existing code that is a
 *      strict prefix of this code. Skipped levels are handled by this rule.
 *   4. Upsert by `code` so re-import is idempotent (AC 2).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NicImportService {

    private final NicCodeRepository nicCodeRepository;
    private final NicTreeService nicTreeService;
    private final AuditService auditService;

    @Transactional
    public NicImportResultResponse importFile(MultipartFile file, String actor) {
        String fileName = file.getOriginalFilename();
        if (fileName == null) throw new BusinessRuleException("File name is required");
        String lower = fileName.toLowerCase();
        List<ParsedRow> rows;
        try (InputStream is = file.getInputStream()) {
            if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
                rows = parseExcel(is);
            } else if (lower.endsWith(".csv")) {
                rows = parseCsv(is);
            } else {
                throw new BusinessRuleException("Unsupported file type: " + fileName);
            }
        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessRuleException("Failed to parse NIC file: " + e.getMessage());
        }

        NicImportResultResponse result = applyRows(rows, actor);
        nicTreeService.invalidate();
        auditService.record("NIC_MASTER_IMPORT", UUID.randomUUID(), actor,
                "IMPORT", null, Map.of(
                        "file_name", fileName,
                        "rows_read", result.rowsRead(),
                        "created", result.created(),
                        "updated", result.updated(),
                        "unresolved_parents", result.unresolvedParents(),
                        "rejected", result.rejected()));
        return result;
    }

    /**
     * Package-private for direct fixture-based tests (unit tests can call
     * this without staging a MultipartFile).
     */
    NicImportResultResponse applyRows(List<ParsedRow> rows, String actor) {
        List<String> rejected = new ArrayList<>();
        List<ParsedRow> valid = new ArrayList<>();
        // First-wins deduplication within a single batch. Some NIC master
        // files (the Sept-2026 upload is one) reuse the same short digit
        // string across sections (e.g. "14" appears as both "Animal
        // production" and "Manufacture of wearing apparel"). The DB has a
        // UNIQUE (code) constraint, so the second occurrence would either
        // overwrite the first or blow up mid-batch. We keep the first row
        // and report the rest so the user knows what was skipped.
        java.util.Set<String> seenInBatch = new java.util.HashSet<>();
        for (ParsedRow r : rows) {
            String code = r.code == null ? null : r.code.trim();
            if (code == null || code.isEmpty() || !code.chars().allMatch(Character::isDigit) || code.length() > 5) {
                rejected.add("row " + r.rowNumber + ": invalid code '" + r.code + "'");
                continue;
            }
            if (r.description == null || r.description.isBlank()) {
                rejected.add("row " + r.rowNumber + ": missing description");
                continue;
            }
            if (!seenInBatch.add(code)) {
                rejected.add("row " + r.rowNumber + ": duplicate code '" + code
                        + "' in this file — kept first occurrence");
                continue;
            }
            valid.add(new ParsedRow(r.rowNumber, code,
                    r.description.trim(),
                    normaliseIndustryType(r.industryType),
                    r.nicDataId));
        }

        // Sort shallow → deep so a parent is always inserted before its
        // children, then the longest-prefix lookup will find it.
        valid.sort(Comparator.comparingInt((ParsedRow p) -> p.code.length())
                .thenComparing(p -> p.code));

        int created = 0;
        int updated = 0;
        int unresolvedParents = 0;

        for (ParsedRow r : valid) {
            Optional<NicCode> existing = nicCodeRepository.findByCode(r.code);
            short level = NicLevelResolver.forCode(r.code);
            UUID parentId = level == 1 ? null : findLongestPrefixParentId(r.code);
            NicCode parentRef = null;
            if (parentId != null) {
                parentRef = nicCodeRepository.findById(parentId).orElse(null);
            } else if (level > 1) {
                unresolvedParents++;
            }

            if (existing.isPresent()) {
                NicCode c = existing.get();
                boolean dirty = false;
                if (!r.description.equals(c.getDescription())) { c.setDescription(r.description); dirty = true; }
                if (!r.industryType.equals(c.getIndustryType())) { c.setIndustryType(r.industryType); dirty = true; }
                if (r.nicDataId != null && !r.nicDataId.equals(c.getNicDataId())) {
                    c.setNicDataId(r.nicDataId); dirty = true;
                }
                if (parentRef != null &&
                        (c.getParent() == null || !parentRef.getId().equals(c.getParent().getId()))) {
                    c.setParent(parentRef); dirty = true;
                }
                if (dirty) {
                    c.setUpdatedBy(actor);
                    nicCodeRepository.save(c);
                    updated++;
                }
            } else {
                nicCodeRepository.save(NicCode.builder()
                        .code(r.code)
                        .description(r.description)
                        .industryType(r.industryType)
                        .level(level)
                        .parent(parentRef)
                        .nicDataId(r.nicDataId)
                        .active(true)
                        .isPrimary(false)
                        .createdBy(actor)
                        .updatedBy(actor)
                        .build());
                created++;
            }
        }

        return new NicImportResultResponse(rows.size(), created, updated,
                unresolvedParents, rejected.size(), rejected);
    }

    private UUID findLongestPrefixParentId(String childCode) {
        // Walk from length-1 shorter down to length 1, checking each prefix.
        for (int len = childCode.length() - 1; len >= 1; len--) {
            String candidate = childCode.substring(0, len);
            Optional<NicCode> parent = nicCodeRepository.findByCode(candidate);
            if (parent.isPresent()) return parent.get().getId();
        }
        return null;
    }

    private String normaliseIndustryType(String raw) {
        if (raw == null) return "Unknown";
        String t = raw.trim().toLowerCase();
        if (t.startsWith("service") || t.equals("s")) return "Service";
        if (t.startsWith("manufactur") || t.startsWith("mfg") || t.equals("m")) return "Manufacturing";
        return "Unknown";
    }

    private List<ParsedRow> parseExcel(InputStream is) throws Exception {
        List<ParsedRow> out = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            var it = sheet.iterator();
            if (!it.hasNext()) return out;
            Row header = it.next();
            Map<String, Integer> hi = headerIndex(header);
            int rowNum = 1;
            while (it.hasNext()) {
                rowNum++;
                Row row = it.next();
                String code = getCell(row, hi, "code");
                String desc = getCell(row, hi, "description");
                String typ  = getCell(row, hi, "industry_type");
                String nid  = getCell(row, hi, "nic_data_id");
                if ((code == null || code.isBlank()) && (desc == null || desc.isBlank())) continue;
                Integer nidInt = null;
                if (nid != null && !nid.isBlank()) {
                    try { nidInt = Integer.parseInt(nid.trim()); } catch (NumberFormatException ignored) {}
                }
                out.add(new ParsedRow(rowNum, code, desc, typ, nidInt));
            }
        }
        return out;
    }

    private List<ParsedRow> parseCsv(InputStream is) throws Exception {
        List<ParsedRow> out = new ArrayList<>();
        try (var reader = new java.io.InputStreamReader(is);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader().setSkipHeaderRecord(true).setTrim(true)
                     .setIgnoreEmptyLines(true).build().parse(reader)) {
            Map<String, Integer> hi = parser.getHeaderMap().entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            e -> canonHeader(e.getKey()),
                            Map.Entry::getValue,
                            (a, b) -> a));
            int rowNum = 1;
            for (CSVRecord record : parser) {
                rowNum++;
                String code = pick(record, hi, "code");
                String desc = pick(record, hi, "description");
                String typ  = pick(record, hi, "industry_type");
                String nid  = pick(record, hi, "nic_data_id");
                if ((code == null || code.isBlank()) && (desc == null || desc.isBlank())) continue;
                Integer nidInt = null;
                if (nid != null && !nid.isBlank()) {
                    try { nidInt = Integer.parseInt(nid.trim()); } catch (NumberFormatException ignored) {}
                }
                out.add(new ParsedRow(rowNum, code, desc, typ, nidInt));
            }
        }
        return out;
    }

    private Map<String, Integer> headerIndex(Row header) {
        Map<String, Integer> hi = new LinkedHashMap<>();
        int i = 0;
        for (Cell c : header) {
            String v = c == null ? null : c.toString();
            if (v != null) hi.putIfAbsent(canonHeader(v), i);
            i++;
        }
        return hi;
    }

    private String getCell(Row row, Map<String, Integer> hi, String logical) {
        Integer idx = pickIndex(hi, logical);
        if (idx == null) return null;
        Cell c = row.getCell(idx);
        if (c == null) return null;
        return switch (c.getCellType()) {
            case STRING  -> c.getStringCellValue().trim();
            case NUMERIC -> {
                double d = c.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) yield String.valueOf((long) d);
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            default      -> "";
        };
    }

    private String pick(CSVRecord record, Map<String, Integer> hi, String logical) {
        Integer idx = pickIndex(hi, logical);
        if (idx == null || idx >= record.size()) return null;
        return record.get(idx);
    }

    private Integer pickIndex(Map<String, Integer> hi, String logical) {
        // Accept obvious column-name variants for each logical field.
        List<String> candidates = switch (logical) {
            case "code"          -> List.of("code", "niccode", "nic5digitid", "nic_5_digit_id", "nic");
            case "description"   -> List.of("description", "desc", "nicdesc", "nicdescription", "activity", "activitydescription", "nicactivity");
            case "industry_type" -> List.of("industry_type", "industrytype", "type", "sector");
            case "nic_data_id"   -> List.of("nic_data_id", "nicdataid", "id", "srno", "slno");
            default              -> List.of();
        };
        for (String c : candidates) {
            Integer i = hi.get(c);
            if (i != null) return i;
        }
        return null;
    }

    private String canonHeader(String h) {
        if (h == null) return "";
        return h.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    /** package-private carrier for parsed rows — reused by unit tests. */
    public record ParsedRow(int rowNumber, String code, String description,
                            String industryType, Integer nicDataId) {}
}
