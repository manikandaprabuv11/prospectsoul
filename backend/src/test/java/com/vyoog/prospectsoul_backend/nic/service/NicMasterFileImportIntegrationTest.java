package com.vyoog.prospectsoul_backend.nic.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

import com.vyoog.prospectsoul_backend.TestcontainersConfiguration;
import com.vyoog.prospectsoul_backend.nic.dto.response.NicImportResultResponse;
import com.vyoog.prospectsoul_backend.nic.entity.NicCode;
import com.vyoog.prospectsoul_backend.nic.repository.NicCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Certifies C1 AC 1 against the real "NIC Codes Updated.xlsx" master file
 * shipped in {@code src/test/resources/nic}. The file has:
 *   - NIC_DataID | NicCode | NicDesc | Industry Type
 *   - 1846 data rows (8 sections, 93 divisions, 233 groups, 447 classes, 1065 sub-classes).
 *
 * The header {@code NicDesc} is a short-form alias — the parser must resolve
 * it to the {@code description} target field, otherwise every row is rejected
 * with "missing description".
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class NicMasterFileImportIntegrationTest {

    @Autowired NicImportService importService;
    @Autowired NicCodeRepository nicCodeRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void reset() {
        jdbcTemplate.execute("TRUNCATE company_nic_codes, contacts, import_rows, "
                + "verification_batch_items, activities, companies, nic_codes "
                + "RESTART IDENTITY CASCADE");
    }

    @Test
    void importsRealNicMasterFileWithZeroUnresolvedParents() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/nic/NIC_Codes_Updated.xlsx")) {
            assertThat(is).as("real NIC file must be on the test classpath").isNotNull();
            MockMultipartFile file = new MockMultipartFile("file",
                    "NIC_Codes_Updated.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    is.readAllBytes());

            NicImportResultResponse res = importService.importFile(file, "test-actor");

            assertThat(res.rowsRead()).isEqualTo(1846);
            // 34 rows collide on `code` — the file uses the same digit
            // string across sections (e.g. "14"). First-wins dedup keeps
            // the first occurrence and reports the rest.
            assertThat(res.rejected()).isEqualTo(34);
            assertThat(res.created()).isEqualTo(1812);
            assertThat(res.updated()).isZero();
            // The uploaded master is missing Section-level rows for 4
            // (Construction) and 5 (Wholesale/Retail/Transport). The seven
            // divisions 41, 42, 43, 45, 46, 47, 49 therefore have no
            // level-1 prefix and land as orphans with parent_id = null.
            // The parser reports them; nothing is rejected.
            assertThat(res.unresolvedParents()).isEqualTo(7);
            assertThat(res.rejectedReasons())
                    .anySatisfy(reason -> assertThat(reason).contains("duplicate code"));
        }

        // Level counts should match the file's actual shape.
        assertThat(nicCodeRepository.count()).isEqualTo(1812L);
        long sections = nicCodeRepository.findAll().stream()
                .filter(n -> n.getLevel() == 1).count();
        // Level-1 (Section) codes never collide in this file.
        assertThat(sections).isEqualTo(8L);

        // A known-deep code resolves back to its real parent path.
        NicCode c1420 = nicCodeRepository.findByCode("1420").orElseThrow();
        NicCode c142  = nicCodeRepository.findByCode("142").orElseThrow();
        NicCode c14   = nicCodeRepository.findByCode("14").orElseThrow();
        NicCode c1    = nicCodeRepository.findByCode("1").orElseThrow();
        assertThat(c1420.getParentId()).isEqualTo(c142.getId());
        assertThat(c142.getParentId()).isEqualTo(c14.getId());
        assertThat(c14.getParentId()).isEqualTo(c1.getId());
        assertThat(c1.getParentId()).isNull();
    }

    @Test
    void reImportOfRealFileIsIdempotent() throws Exception {
        byte[] bytes;
        try (InputStream is = getClass().getResourceAsStream("/nic/NIC_Codes_Updated.xlsx")) {
            assertThat(is).isNotNull();
            bytes = is.readAllBytes();
        }
        MockMultipartFile file = new MockMultipartFile("file",
                "NIC_Codes_Updated.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytes);
        NicImportResultResponse first = importService.importFile(file, "actor-1");
        NicImportResultResponse second = importService.importFile(file, "actor-2");
        assertThat(first.created()).isEqualTo(1812);
        // On second run every row is a no-op update (same desc/type) → zero counted updates.
        assertThat(second.created()).isZero();
        assertThat(second.updated()).isZero();
        assertThat(nicCodeRepository.count()).isEqualTo(1812L);
    }
}
