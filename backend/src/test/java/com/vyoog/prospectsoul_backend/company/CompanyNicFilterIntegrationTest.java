package com.vyoog.prospectsoul_backend.company;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.TestcontainersConfiguration;
import com.vyoog.prospectsoul_backend.company.dto.response.CompanyGroupedByNicResponse;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.nic.entity.CompanyNicCode;
import com.vyoog.prospectsoul_backend.company.nic.repository.CompanyNicCodeRepository;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.company.service.CompanyService;
import com.vyoog.prospectsoul_backend.company.specification.CompanySpecification;
import com.vyoog.prospectsoul_backend.nic.entity.NicCode;
import com.vyoog.prospectsoul_backend.nic.repository.NicCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CompanyNicFilterIntegrationTest {

    @Autowired CompanyService companyService;
    @Autowired CompanyRepository companyRepository;
    @Autowired CompanyNicCodeRepository joinRepository;
    @Autowired NicCodeRepository nicCodeRepository;

    @BeforeEach
    void cleanState() {
        joinRepository.deleteAll();
        companyRepository.deleteAll();
        nicCodeRepository.deleteAll();
    }

    private NicCode seed(String code, String desc, NicCode parent) {
        return nicCodeRepository.save(NicCode.builder()
                .code(code).description(desc).industryType("Manufacturing")
                .level((short) code.length())
                .parent(parent)
                .active(true).isPrimary(false).build());
    }

    private Company companyWith(String name, NicCode... classifications) {
        Company c = companyRepository.save(Company.builder()
                .canonicalName(name).normalizedName(name.toLowerCase()).source("MANUAL").build());
        short seq = 1;
        for (NicCode n : classifications) {
            joinRepository.save(CompanyNicCode.builder()
                    .companyId(c.getId())
                    .nicCode(n)
                    .nicCodeRaw(n.getCode())
                    .sequenceNo(seq)
                    .isPrimary(seq == 1)
                    .build());
            seq++;
        }
        return c;
    }

    @Test
    void filterByNicParentIncludesAllDescendantsExactCounts() {
        NicCode n22    = seed("22",    "Rubber and plastics", null);
        NicCode n221   = seed("221",   "Rubber products", n22);
        NicCode n222   = seed("222",   "Plastics products", n22);
        NicCode n2211  = seed("2211",  "Tyres and tubes", n221);
        NicCode n33    = seed("33",    "Repair", null);

        // 3 companies under n22 (including via descendants), 1 under n33
        companyWith("Rubber Co", n221);
        companyWith("Plastics Co", n222);
        companyWith("Tyre Co", n2211);
        companyWith("Repair Co", n33);

        var descendants = companyService.expandNicSubtree(n22.getId());
        assertThat(descendants).containsExactlyInAnyOrder(n22.getId(), n221.getId(), n222.getId(), n2211.getId());

        var filters = new CompanySpecification.Filters(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, descendants, null);
        var page = companyService.listWithFilters(filters, 0, 100, "createdAt", "desc");
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.content()).extracting("canonicalName")
                .containsExactlyInAnyOrder("Rubber Co", "Plastics Co", "Tyre Co");

        // include_descendants = false — only direct hits on the parent node (0 here)
        var filtersDirect = new CompanySpecification.Filters(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, List.of(n22.getId()), null);
        var pageDirect = companyService.listWithFilters(filtersDirect, 0, 100, "createdAt", "desc");
        assertThat(pageDirect.totalElements()).isEqualTo(0);
    }

    @Test
    void groupedViewReturnsPerChildCountsPlusDirectlyTaggedBucket() {
        NicCode n22   = seed("22",  "Rubber and plastics", null);
        NicCode n221  = seed("221", "Rubber products", n22);
        NicCode n222  = seed("222", "Plastics products", n22);
        companyWith("Rubber-1", n221);
        companyWith("Rubber-2", n221);
        companyWith("Plastics-1", n222);
        companyWith("Untagged Sub", n22);   // directly tagged to the parent

        CompanyGroupedByNicResponse resp = companyService.groupedByNic(n22.getId(),
                new CompanySpecification.Filters(null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null,
                        null, null, null));

        assertThat(resp.view()).isEqualTo("grouped_by_nic");
        assertThat(resp.rootNode().code()).isEqualTo("22");
        assertThat(resp.totalCompanies()).isEqualTo(4);
        assertThat(resp.groups()).hasSize(3);

        var byLabel = resp.groups().stream().collect(
                java.util.stream.Collectors.toMap(g -> g.node() == null ? "direct" : g.node().code(), g -> g.count()));
        assertThat(byLabel.get("221")).isEqualTo(2L);
        assertThat(byLabel.get("222")).isEqualTo(1L);
        assertThat(byLabel.get("direct")).isEqualTo(1L);
    }
}
