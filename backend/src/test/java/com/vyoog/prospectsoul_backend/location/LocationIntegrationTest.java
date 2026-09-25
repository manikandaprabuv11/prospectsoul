package com.vyoog.prospectsoul_backend.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import com.vyoog.prospectsoul_backend.TestcontainersConfiguration;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.nic.entity.CompanyNicCode;
import com.vyoog.prospectsoul_backend.company.nic.repository.CompanyNicCodeRepository;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.imports.entity.ImportBatch;
import com.vyoog.prospectsoul_backend.imports.repository.ImportBatchRepository;
import com.vyoog.prospectsoul_backend.imports.repository.ImportRowRepository;
import com.vyoog.prospectsoul_backend.location.dto.response.MapCompanyResponse;
import com.vyoog.prospectsoul_backend.location.dto.response.PincodeCentroidResponse;
import com.vyoog.prospectsoul_backend.location.service.CompanyMapService;
import com.vyoog.prospectsoul_backend.location.service.PincodeCentroidService;
import com.vyoog.prospectsoul_backend.nic.entity.NicCode;
import com.vyoog.prospectsoul_backend.nic.repository.NicCodeRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LocationIntegrationTest {

    @Autowired PincodeCentroidService pincodeCentroidService;
    @Autowired CompanyMapService companyMapService;
    @Autowired CompanyRepository companyRepository;
    @Autowired ImportBatchRepository importBatchRepository;
    @Autowired ImportRowRepository importRowRepository;
    @Autowired NicCodeRepository nicCodeRepository;
    @Autowired CompanyNicCodeRepository companyNicCodeRepository;
    @Autowired MockMvc mockMvc;

    private static final String ANALYST_UUID = "a1000000-0000-0000-0000-000000000001";

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor analystJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_PS_ANALYST"))
                .jwt(jwt -> jwt.subject(ANALYST_UUID).claim("preferred_username", "analyst"));
    }


    @Test
    void pincodeCentroidComesFromSeededTableNotFromGoogle() {
        PincodeCentroidResponse resp = pincodeCentroidService.lookup("641001");
        assertThat(resp.state()).isEqualTo("Tamil Nadu");
        assertThat(resp.centroid().lat()).isEqualByComparingTo(new BigDecimal("11.017100"));
    }



    @Test
    void mapCompaniesReturnsOwnedRowsWithinRadius() {
        // Coimbatore centroid at (11.0171, 76.9587); place a company inside 2km.
        Company inside = companyRepository.save(Company.builder()
                .canonicalName("Near Company")
                .normalizedName("near company")
                .source("MANUAL")
                .pincode("641001")
                .latitude(new BigDecimal("11.020000"))
                .longitude(new BigDecimal("76.960000"))
                .build());
        Company outside = companyRepository.save(Company.builder()
                .canonicalName("Far Company")
                .normalizedName("far company")
                .source("MANUAL")
                .pincode("110001")
                .latitude(new BigDecimal("28.632700"))
                .longitude(new BigDecimal("77.219700"))
                .build());

        MapCompanyResponse resp = companyMapService.companiesInPincode("641001", 5);
        assertThat(resp.content()).extracting("id").contains(inside.getId());
        assertThat(resp.content()).extracting("id").doesNotContain(outside.getId());
    }

    @Test
    void unknownPincodeThrowsResourceNotFoundWhenGeocoderCannotResolveIt() {
        // The NoOpPincodeGeocoder is wired in tests that don't import
        // TestPincodeGeocoderConfig, so every un-seeded pincode is treated
        // as unknown and the caller sees a proper 404. Previously the
        // service used to return a graceful India-centroid stub; the new
        // resolver rejects unknown pincodes so the user gets an actionable
        // "pincode not found" instead of a plot at (20, 78) they can never
        // interpret.
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> pincodeCentroidService.lookup("603102"))
                .isInstanceOf(com.vyoog.prospectsoul_backend.common.exception.ResourceNotFoundException.class)
                .hasMessageContaining("603102");
    }

    @Test
    void mapCompaniesForUnseededPincodeStillReturnsMatchingRows() {
        Company c = companyRepository.save(Company.builder()
                .canonicalName("Kanchi Co").normalizedName("kanchi co")
                .source("MANUAL").pincode("603102").build());

        MapCompanyResponse resp = companyMapService.companiesInPincode("603102", 5);
        assertThat(resp.unknownPincode()).isTrue();
        assertThat(resp.content()).extracting("id").contains(c.getId());
    }

    // ---- Map NIC filter parity with Companies list (multi-select + bad UUID) ----

    @Test
    void mapCompanies_twoNicParentIds_orsAcrossSelectionWithinPincode() throws Exception {
        NicCode nicA = saveNic("94", "Map OR-select A");
        NicCode nicB = saveNic("95", "Map OR-select B");
        NicCode unrelated = saveNic("96", "Map OR-select unrelated");

        String companyA = createPincodeCompanyWithNic("Map OR Match A Corp", "641050", nicA);
        String companyB = createPincodeCompanyWithNic("Map OR Match B Corp", "641050", nicB);
        String companyOutside = createPincodeCompanyWithNic("Map OR Non-Match Corp", "641050", unrelated);

        // Single nic_parent_ids -> only companyA.
        mockMvc.perform(get("/api/v1/map/companies")
                        .with(analystJwt())
                        .param("pincode", "641050")
                        .param("nic_parent_ids", nicA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", Matchers.hasItem(companyA)))
                .andExpect(jsonPath("$.content[*].id", Matchers.not(Matchers.hasItem(companyB))))
                .andExpect(jsonPath("$.content[*].id", Matchers.not(Matchers.hasItem(companyOutside))));

        // Two nic_parent_ids -> union: both A and B, still excluding the unrelated NIC.
        mockMvc.perform(get("/api/v1/map/companies")
                        .with(analystJwt())
                        .param("pincode", "641050")
                        .param("nic_parent_ids", nicA.getId().toString(), nicB.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", Matchers.hasItem(companyA)))
                .andExpect(jsonPath("$.content[*].id", Matchers.hasItem(companyB)))
                .andExpect(jsonPath("$.content[*].id", Matchers.not(Matchers.hasItem(companyOutside))));
    }

    @Test
    void mapCompanies_badNicParentIdUuid_returns422WithTokenInDetail() throws Exception {
        mockMvc.perform(get("/api/v1/map/companies")
                        .with(analystJwt())
                        .param("pincode", "641050")
                        .param("nic_parent_ids", "not-a-uuid"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", Matchers.containsString("not-a-uuid")));
    }

    private NicCode saveNic(String code, String description) {
        return nicCodeRepository.save(NicCode.builder()
                .code(code)
                .description(description)
                .industryType("Manufacturing")
                .level((short) 1)
                .isPrimary(false)
                .active(true)
                .build());
    }

    private String createPincodeCompanyWithNic(String name, String pincode, NicCode nic) {
        Company company = companyRepository.save(Company.builder()
                .canonicalName(name)
                .normalizedName(name.toLowerCase())
                .source("MANUAL")
                .pincode(pincode)
                .build());
        companyNicCodeRepository.save(CompanyNicCode.builder()
                .companyId(company.getId())
                .nicCode(nic)
                .nicCodeRaw(nic.getCode())
                .isPrimary(true)
                .sequenceNo((short) 1)
                .createdAt(Instant.now())
                .build());
        return company.getId().toString();
    }
}
