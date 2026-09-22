package com.vyoog.prospectsoul_backend.imports;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.vyoog.prospectsoul_backend.TestcontainersConfiguration;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.nic.repository.CompanyNicCodeRepository;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.imports.dto.request.ColumnMappingRequest;
import com.vyoog.prospectsoul_backend.imports.dto.response.ImportBatchResponse;
import com.vyoog.prospectsoul_backend.imports.entity.ImportBatch;
import com.vyoog.prospectsoul_backend.imports.entity.ImportRow;
import com.vyoog.prospectsoul_backend.imports.repository.ImportBatchRepository;
import com.vyoog.prospectsoul_backend.imports.repository.ImportRowRepository;
import com.vyoog.prospectsoul_backend.imports.service.ImportProcessingService;
import com.vyoog.prospectsoul_backend.imports.service.ImportService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;

import java.util.concurrent.TimeUnit;

/**
 * Exercises the Sales-Intelligence import path end-to-end with a
 * synthetic Kanchipuram-style .xlsx:
 *   - Activities JSON parsed into company_nic_codes rows,
 *   - dedup rule ⓪ (source_reference) fires on re-import,
 *   - malformed Activities do NOT fail the row,
 *   - blank name rejects with outcome_reason = missing_name.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UdyamImportIntegrationTest {

    @Autowired ImportService importService;
    @Autowired ImportProcessingService processingService;
    @Autowired ImportBatchRepository batchRepository;
    @Autowired ImportRowRepository rowRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired CompanyNicCodeRepository joinRepository;

    private byte[] buildKanchipuramSample() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sheet = wb.createSheet("Sheet1");
            String[] headers = {"LG_ST_Code", "State", "LG_DT_Code", "District",
                    "Pincode", "RegistrationDate", "EnterpriseName",
                    "CommunicationAddress", "Activities"};
            var header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);

            Object[][] rows = new Object[][] {
                    {33, "Tamil Nadu", 601, "Kanchipuram", "631502", "05/01/2023",
                            "ABC Pumps Pvt Ltd", "12 Industrial Road",
                            "[{\"NIC5DigitId\":\"28131\",\"Description\":\"Hand pumps\"}," +
                             "{\"NIC5DigitId\":\"28132\",\"Description\":\"Other pumps\"}]"},
                    {33, "Tamil Nadu", 601, "Kanchipuram", "631502", "06/01/2023",
                            "XYZ Plastics", "5 SIDCO Estate",
                            "[{\"NIC5DigitId\":\"22201\",\"Description\":\"Plastic sheet\"}]"},
                    {33, "Tamil Nadu", 601, "Kanchipuram", "631502", "07/01/2023",
                            "Bad JSON Co", "9 Main Road", "NA"},
                    {33, "Tamil Nadu", 601, "Kanchipuram", "631502", "08/01/2023",
                            "Broken JSON Co", "9 Main Road", "[{oops"},
                    {33, "Tamil Nadu", 601, "Kanchipuram", "631502", "09/01/2023",
                            "", "10 Main Road", "[]"}
            };
            for (int r = 0; r < rows.length; r++) {
                var row = sheet.createRow(r + 1);
                for (int c = 0; c < rows[r].length; c++) {
                    Object v = rows[r][c];
                    if (v instanceof Number n) row.createCell(c).setCellValue(n.doubleValue());
                    else row.createCell(c).setCellValue(String.valueOf(v));
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void kanchipuramSyntheticImportCreatesCompaniesAndNicJoinsAndFiresDedupOnReImport() throws Exception {
        byte[] bytes = buildKanchipuramSample();

        MockMultipartFile file = new MockMultipartFile("file",
                "Kanchipuram_sample.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new ByteArrayInputStream(bytes).readAllBytes());

        ImportBatchResponse batch = importService.upload(file, "UDYAM_MSME_REGISTRY", "actor");
        assertThat(batch.totalRows()).isEqualTo(5);

        Map<String, String> mappings = new LinkedHashMap<>();
        mappings.put("LG_ST_Code", "lg_state_code");
        mappings.put("State", "state");
        mappings.put("LG_DT_Code", "lg_district_code");
        mappings.put("District", "district");
        mappings.put("Pincode", "pincode");
        mappings.put("RegistrationDate", "registration_date");
        mappings.put("EnterpriseName", "canonical_name");
        mappings.put("CommunicationAddress", "address_line");
        mappings.put("Activities", "activities_json");

        importService.confirmMappings(batch.id(), toColumnMappingRequest(mappings), "actor");
        importService.startProcessing(batch.id(), "actor");
        // Wait for the async worker to settle.
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> {
            var b = batchRepository.findById(batch.id()).orElseThrow();
            return b.getStatus() == ImportBatch.BatchStatus.COMPLETED
                || b.getStatus() == ImportBatch.BatchStatus.FAILED;
        });

        var finalBatch = batchRepository.findById(batch.id()).orElseThrow();
        assertThat(finalBatch.getStatus()).isEqualTo(ImportBatch.BatchStatus.COMPLETED);
        assertThat(finalBatch.getCreatedRows()).isEqualTo(4);
        assertThat(finalBatch.getRejectedRows()).isEqualTo(1);

        // Verify per-row outcomes.
        List<ImportRow> rows = rowRepository.findAll();
        long invalidJson = rows.stream()
                .filter(r -> "activities_json_invalid".equals(r.getOutcomeReason())).count();
        long missingName = rows.stream()
                .filter(r -> "missing_name".equals(r.getOutcomeReason())).count();
        assertThat(invalidJson).isEqualTo(1);
        assertThat(missingName).isEqualTo(1);

        Company abc = companyRepository.findAll().stream()
                .filter(c -> c.getCanonicalName().equals("ABC Pumps Pvt Ltd")).findFirst().orElseThrow();
        assertThat(abc.getPincode()).isEqualTo("631502");
        assertThat(abc.getDistrict()).isEqualTo("Kanchipuram");
        assertThat(abc.getSourceReference()).isNotNull();
        // Two NIC join rows expected — unresolved (master empty), raw preserved.
        var abcJoins = joinRepository.findByCompanyIdOrderBySequenceNoAsc(abc.getId());
        assertThat(abcJoins).hasSize(2);
        assertThat(abcJoins.get(0).getNicCodeRaw()).isEqualTo("28131");
        assertThat(abcJoins.get(0).getIsPrimary()).isTrue();

        // --- Re-import: dedup rule 0 should fire on every non-empty-name row ---
        MockMultipartFile file2 = new MockMultipartFile("file",
                "Kanchipuram_sample.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytes);
        ImportBatchResponse batch2 = importService.upload(file2, "UDYAM_MSME_REGISTRY", "actor");
        importService.confirmMappings(batch2.id(), toColumnMappingRequest(mappings), "actor");
        importService.startProcessing(batch2.id(), "actor");
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> {
            var b = batchRepository.findById(batch2.id()).orElseThrow();
            return b.getStatus() == ImportBatch.BatchStatus.COMPLETED
                || b.getStatus() == ImportBatch.BatchStatus.FAILED;
        });

        var b2 = batchRepository.findById(batch2.id()).orElseThrow();
        assertThat(b2.getStatus()).isEqualTo(ImportBatch.BatchStatus.COMPLETED);
        assertThat(b2.getDuplicateRows()).isEqualTo(4);
        assertThat(b2.getCreatedRows()).isEqualTo(0);
    }

    private ColumnMappingRequest toColumnMappingRequest(Map<String, String> mappings) {
        var entries = mappings.entrySet().stream()
                .map(e -> new ColumnMappingRequest.MappingEntry(e.getKey(), e.getValue()))
                .toList();
        return new ColumnMappingRequest(entries);
    }
}
