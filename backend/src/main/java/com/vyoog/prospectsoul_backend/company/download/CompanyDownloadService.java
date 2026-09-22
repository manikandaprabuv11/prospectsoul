package com.vyoog.prospectsoul_backend.company.download;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.common.exception.BusinessRuleException;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.company.specification.CompanySpecification;
import com.vyoog.prospectsoul_backend.contact.entity.Contact;
import com.vyoog.prospectsoul_backend.contact.repository.ContactRepository;
import com.vyoog.prospectsoul_backend.nic.repository.NicCodeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-0005: filter-scoped download that is NOT the pipeline Export.
 * Writes NO row to {@code companies}, {@code exports} or any other table
 * except {@code audit_log}. The audit row captures who, when, the filter
 * snapshot and the row count — enough to explain any later data-flow question.
 */
@Service
@RequiredArgsConstructor
public class CompanyDownloadService {

    private static final List<String> DEFAULT_COLUMNS = List.of(
            "canonical_name", "primary_phone_normalized", "email", "website_domain",
            "city", "state", "district", "region", "pincode", "primary_nic_code",
            "turnover", "employee_count", "gst_number", "pipeline_state",
            "verification_status", "primary_contact_name", "primary_contact_role");

    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final NicCodeRepository nicCodeRepository;
    private final AuditService auditService;

    @Value("${prospectsoul.download.row-cap:10000}")
    private int rowCap;

    public record DownloadResult(byte[] bytes, String contentType, String fileName) {}

    @Transactional
    public DownloadResult download(CompanyDownloadRequest req, String actor) {
        String format = req.format() == null ? "csv" : req.format().toLowerCase();
        List<String> columns = req.columns() != null && !req.columns().isEmpty()
                ? req.columns() : DEFAULT_COLUMNS;
        boolean allContacts = Boolean.TRUE.equals(req.includeAllContacts());

        CompanySpecification.Filters filters = toFilters(req.filter());
        long count = companyRepository.count(CompanySpecification.withFilters(filters));
        if (count > rowCap) {
            throw new BusinessRuleException(
                    "Filter matches " + count + " companies; the download cap is "
                            + rowCap + ". Please narrow your filter.");
        }

        List<Company> companies = companyRepository.findAll(
                CompanySpecification.withFilters(filters), Sort.by("canonicalName"));

        byte[] bytes;
        String contentType;
        String fileName;
        try {
            if ("xlsx".equals(format)) {
                bytes = writeXlsx(companies, columns, allContacts);
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                fileName = "companies.xlsx";
            } else {
                bytes = writeCsv(companies, columns, allContacts);
                contentType = "text/csv;charset=UTF-8";
                fileName = "companies.csv";
            }
        } catch (IOException e) {
            throw new BusinessRuleException("Failed to produce download: " + e.getMessage());
        }

        auditService.record("COMPANY_DOWNLOAD", UUID.randomUUID(), actor, "DOWNLOAD",
                null, Map.of(
                        "format", format,
                        "row_count", companies.size(),
                        "include_all_contacts", allContacts,
                        "columns", columns,
                        "filter", req.filter() == null ? Map.of() : req.filter()));

        return new DownloadResult(bytes, contentType, fileName);
    }

    private byte[] writeCsv(List<Company> companies, List<String> columns, boolean allContacts) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (OutputStreamWriter w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter p = new CSVPrinter(w, CSVFormat.DEFAULT.builder().setHeader(columns.toArray(String[]::new)).build())) {
            for (Company c : companies) {
                List<Contact> contacts = allContacts
                        ? contactRepository.findByCompanyIdOrderByIsPrimaryDescCreatedAtAsc(c.getId())
                        : contactRepository.findFirstByCompanyIdAndIsPrimaryTrue(c.getId())
                            .map(List::of).orElseGet(() -> contactRepository
                                .findByCompanyIdOrderByIsPrimaryDescCreatedAtAsc(c.getId()).stream().limit(1).toList());
                if (contacts.isEmpty()) contacts = java.util.Arrays.asList((Contact) null);
                for (Contact contact : contacts) {
                    List<Object> row = new ArrayList<>(columns.size());
                    for (String col : columns) row.add(cell(c, contact, col));
                    p.printRecord(row);
                }
            }
        }
        return out.toByteArray();
    }

    private byte[] writeXlsx(List<Company> companies, List<String> columns, boolean allContacts) throws IOException {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("Companies");
            Row header = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) header.createCell(i).setCellValue(columns.get(i));

            int r = 1;
            for (Company c : companies) {
                List<Contact> contacts = allContacts
                        ? contactRepository.findByCompanyIdOrderByIsPrimaryDescCreatedAtAsc(c.getId())
                        : contactRepository.findFirstByCompanyIdAndIsPrimaryTrue(c.getId())
                            .map(List::of).orElseGet(() -> contactRepository
                                .findByCompanyIdOrderByIsPrimaryDescCreatedAtAsc(c.getId()).stream().limit(1).toList());
                if (contacts.isEmpty()) contacts = new ArrayList<>() {{ add(null); }};
                for (Contact contact : contacts) {
                    Row row = sheet.createRow(r++);
                    for (int i = 0; i < columns.size(); i++) {
                        Object v = cell(c, contact, columns.get(i));
                        row.createCell(i).setCellValue(v == null ? "" : String.valueOf(v));
                    }
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            wb.dispose();
            return out.toByteArray();
        }
    }

    private Object cell(Company c, Contact contact, String col) {
        return switch (col) {
            case "canonical_name"           -> c.getCanonicalName();
            case "primary_phone_normalized" -> c.getPrimaryPhoneNormalized();
            case "email"                    -> c.getEmail();
            case "website_domain"           -> c.getWebsiteDomain();
            case "city"                     -> c.getCity();
            case "state"                    -> c.getState();
            case "district"                 -> c.getDistrict();
            case "region"                   -> c.getRegion();
            case "pincode"                  -> c.getPincode();
            case "turnover"                 -> c.getTurnover() == null ? "" : c.getTurnover().toPlainString();
            case "employee_count"           -> c.getEmployeeCount() == null ? "" : c.getEmployeeCount().toString();
            case "gst_number"               -> c.getGstNumber();
            case "pipeline_state"           -> c.getPipelineState().name();
            case "verification_status"      -> c.getVerificationStatus().name();
            case "primary_nic_code"         -> c.getPrimaryNicCodeId() == null ? "" :
                    nicCodeRepository.findById(c.getPrimaryNicCodeId()).map(n -> n.getCode()).orElse("");
            case "primary_contact_name"     -> contact == null ? "" : contact.getName();
            case "primary_contact_role"     -> contact == null ? "" :
                    (contact.getRole() == null ? "" : contact.getRole().getLabel());
            case "primary_contact_phone"    -> contact == null ? "" : contact.getPhone();
            case "primary_contact_email"    -> contact == null ? "" : contact.getEmail();
            default                         -> "";
        };
    }

    private CompanySpecification.Filters toFilters(CompanyDownloadRequest.Filter f) {
        if (f == null) return new CompanySpecification.Filters(null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null);
        Collection<UUID> nicIds = null;
        if (f.nicParentId() != null) {
            boolean include = f.nicIncludeDescendants() == null || f.nicIncludeDescendants();
            nicIds = include ? nicCodeRepository.findDescendantIds(f.nicParentId()) : List.of(f.nicParentId());
        }
        return new CompanySpecification.Filters(
                f.q(), f.city(), f.state(), f.industry(), f.cluster(), f.source(),
                f.pipelineState(), f.verificationStatus(),
                f.region(), f.district(), f.pincode(),
                f.turnoverMin(), f.turnoverMax(), f.employeeMin(), f.employeeMax(), f.gstPresent(),
                f.nicCodeId(), nicIds, f.hasContactRoleId());
    }
}
