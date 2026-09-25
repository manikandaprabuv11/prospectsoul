package com.vyoog.prospectsoul_backend.nic.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.vyoog.prospectsoul_backend.TestcontainersConfiguration;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.nic.entity.CompanyNicCode;
import com.vyoog.prospectsoul_backend.nic.dto.response.NicImportResultResponse;
import com.vyoog.prospectsoul_backend.nic.entity.NicCode;
import com.vyoog.prospectsoul_backend.nic.repository.NicCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exercises the longest-prefix parent resolution (Kickoff constraint 5),
 * skipped-level handling, and idempotent re-import (C1 AC 2, AC 3).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class NicImportServiceIntegrationTest {

    @Autowired NicImportService importService;
    @Autowired NicCodeRepository nicCodeRepository;
    @Autowired com.vyoog.prospectsoul_backend.company.nic.repository.CompanyNicCodeRepository companyNicCodeRepository;
    @Autowired com.vyoog.prospectsoul_backend.company.repository.CompanyRepository companyRepository;

    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @jakarta.persistence.PersistenceContext
    jakarta.persistence.EntityManager entityManager;

    @BeforeEach
    void reset() {
        // TRUNCATE the whole graph that could reference nic_codes so a
        // clean run of this test never trips over stale rows left by
        // other test classes.
        jdbcTemplate.execute("TRUNCATE company_nic_codes, contacts, import_rows, verification_batch_items, activities, companies, nic_codes RESTART IDENTITY CASCADE");
    }

    @Test
    void importsAtEveryLevelAndResolvesParentsByLongestPrefix() {
        List<NicImportService.ParsedRow> rows = List.of(
                new NicImportService.ParsedRow(1, "2",     "Manufacturing (Section)",  "Manufacturing", 0),
                new NicImportService.ParsedRow(2, "22",    "Rubber and plastics",      "Manufacturing", 1),
                new NicImportService.ParsedRow(3, "221",   "Rubber products",          "Manufacturing", 2),
                new NicImportService.ParsedRow(4, "222",   "Plastics products",        "Manufacturing", 3),
                new NicImportService.ParsedRow(5, "2211",  "Rubber tyres and tubes",   "Manufacturing", 4),
                new NicImportService.ParsedRow(6, "22111", "Manufacture of tyres",     "Manufacturing", 5),
                // Skipped-level case: 3-digit parent, 5-digit child, no 4-digit link.
                new NicImportService.ParsedRow(7, "22201", "Plastic sheet",            "Manufacturing", 6)
        );

        NicImportResultResponse res = importService.applyRows(rows, "test-actor");

        assertThat(res.rowsRead()).isEqualTo(7);
        assertThat(res.created()).isEqualTo(7);
        assertThat(res.updated()).isZero();
        assertThat(res.rejected()).isZero();
        // Only "2" is top-level (Section); every other row must find a real parent.
        assertThat(res.unresolvedParents()).isZero();

        NicCode c2     = nicCodeRepository.findByCode("2").orElseThrow();
        NicCode c22    = nicCodeRepository.findByCode("22").orElseThrow();
        NicCode c221   = nicCodeRepository.findByCode("221").orElseThrow();
        NicCode c222   = nicCodeRepository.findByCode("222").orElseThrow();
        NicCode c2211  = nicCodeRepository.findByCode("2211").orElseThrow();
        NicCode c22111 = nicCodeRepository.findByCode("22111").orElseThrow();
        NicCode c22201 = nicCodeRepository.findByCode("22201").orElseThrow();

        assertThat(c2.getParentId()).isNull();
        assertThat(c22.getParentId()).isEqualTo(c2.getId());
        assertThat(c221.getParentId()).isEqualTo(c22.getId());
        assertThat(c222.getParentId()).isEqualTo(c22.getId());
        assertThat(c2211.getParentId()).isEqualTo(c221.getId());
        assertThat(c22111.getParentId()).isEqualTo(c2211.getId());
        // Skipped-level branch: parent falls back to the longest existing
        // prefix, which is 222 (no 2220 row exists).
        assertThat(c22201.getParentId()).isEqualTo(c222.getId());

        assertThat(c22.getLevel()).isEqualTo((short) 2);
        assertThat(c22111.getLevel()).isEqualTo((short) 5);
    }

    @Test
    void reImportIsIdempotentAndCountsUpdates() {
        List<NicImportService.ParsedRow> rows = List.of(
                new NicImportService.ParsedRow(1, "01", "Crop and animal production", "Service", null),
                new NicImportService.ParsedRow(2, "014", "Animal production",         "Service", null)
        );

        NicImportResultResponse first = importService.applyRows(rows, "actor-1");
        assertThat(first.created()).isEqualTo(2);
        assertThat(first.updated()).isZero();

        NicImportResultResponse second = importService.applyRows(rows, "actor-2");
        assertThat(second.created()).isZero();
        assertThat(second.updated()).isZero();

        // Description change should count as an update on a subsequent import.
        List<NicImportService.ParsedRow> patched = List.of(
                new NicImportService.ParsedRow(1, "01", "Crop and animal production (v2)", "Service", null)
        );
        NicImportResultResponse third = importService.applyRows(patched, "actor-3");
        assertThat(third.created()).isZero();
        assertThat(third.updated()).isEqualTo(1);
    }

    @Test
    void rejectsRowsWithMissingCodeOrDescription() {
        List<NicImportService.ParsedRow> rows = List.of(
                new NicImportService.ParsedRow(1, "",   "no code",  "Service", null),
                new NicImportService.ParsedRow(2, "01", "",          "Service", null),
                new NicImportService.ParsedRow(3, "01", "Crop and animal production", "Service", null)
        );

        NicImportResultResponse res = importService.applyRows(rows, "actor");
        assertThat(res.rejected()).isEqualTo(2);
        assertThat(res.created()).isEqualTo(1);
    }

    /**
     * Reproduces the root cause behind "pincode + NIC returns empty even
     * though companies obviously match both": when a company (and its
     * {@code company_nic_codes} join row) is imported BEFORE the matching
     * NIC master code exists, {@code ImportProcessingService}'s exact-code
     * lookup misses and {@code nic_code_id} / {@code primary_nic_code_id}
     * are left NULL — permanently, since nothing ever revisited them. Every
     * NIC-scoped query (Companies page, Map) joins on that FK, so the
     * company silently becomes invisible to NIC filtering.
     * <p>
     * Uploading the NIC master (even after the fact) must retroactively
     * repair this: the orphaned join row and the company's primary pointer
     * should both resolve as soon as the matching code appears.
     */
    @Test
    void importingNicMaster_backfillsPreviouslyOrphanedCompanyNicLinks() throws Exception {
        Company company = companyRepository.saveAndFlush(Company.builder()
                .canonicalName("Orphaned Nic Link Corp")
                .normalizedName("orphaned nic link corp")
                .build());

        CompanyNicCode orphanRow = companyNicCodeRepository.saveAndFlush(CompanyNicCode.builder()
                .companyId(company.getId())
                .nicCode(null) // unresolved at import time — the master didn't have this code yet
                .nicCodeRaw("22111")
                .isPrimary(true)
                .sequenceNo((short) 1)
                .build());

        assertThat(orphanRow.getNicCode()).isNull();
        assertThat(company.getPrimaryNicCodeId()).isNull();

        // Same two steps importFile() runs in sequence (applyRows, then the
        // backfill) — going through applyRows directly here, exactly like
        // the fixture-based tests above, avoids coupling this test to the
        // file-parsing layer, which isn't what's under test.
        List<NicImportService.ParsedRow> rows = List.of(
                new NicImportService.ParsedRow(1, "22111", "Manufacture of tyres", "Manufacturing", null));
        importService.applyRows(rows, "actor");
        entityManager.flush(); // same flush importFile() does before the backfill
        importService.backfillCompanyNicResolution();

        // The backfill runs as raw JDBC UPDATEs alongside the JPA-managed
        // import — clear the persistence context so the re-reads below hit
        // the database instead of returning the stale, pre-backfill entities
        // already sitting in the first-level cache.
        entityManager.clear();

        NicCode resolved = nicCodeRepository.findByCode("22111").orElseThrow();

        CompanyNicCode reloaded = companyNicCodeRepository.findById(orphanRow.getId()).orElseThrow();
        assertThat(reloaded.getNicCodeId()).isEqualTo(resolved.getId());

        Company reloadedCompany = companyRepository.findById(company.getId()).orElseThrow();
        assertThat(reloadedCompany.getPrimaryNicCodeId()).isEqualTo(resolved.getId());
    }
}
