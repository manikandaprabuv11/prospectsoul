package com.vyoog.prospectsoul_backend.company;

import java.util.List;
import java.util.UUID;

import tools.jackson.databind.ObjectMapper;
import com.vyoog.prospectsoul_backend.TestcontainersConfiguration;
import com.vyoog.prospectsoul_backend.company.defaultfilter.dto.request.CompanyDefaultFilterUpsertRequest;
import com.vyoog.prospectsoul_backend.company.defaultfilter.dto.response.CompanyDefaultFilterResponse;
import com.vyoog.prospectsoul_backend.company.dto.request.CompanyCreateRequest;
import com.vyoog.prospectsoul_backend.company.dto.request.CompanyUpdateRequest;
import com.vyoog.prospectsoul_backend.company.nic.dto.request.AttachNicCodeRequest;
import com.vyoog.prospectsoul_backend.nic.entity.NicCode;
import com.vyoog.prospectsoul_backend.nic.repository.NicCodeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CompanyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NicCodeRepository nicCodeRepository;

    private static final String ANALYST_UUID = "a1000000-0000-0000-0000-000000000001";
    private static final String VIEWER_UUID = "d4000000-0000-0000-0000-000000000001";
    private static final String ADMIN_UUID = "c3000000-0000-0000-0000-000000000001";

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor analystJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_PS_ANALYST"))
                .jwt(jwt -> jwt.subject(ANALYST_UUID).claim("preferred_username", "analyst"));
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor viewerJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_PS_VIEWER"))
                .jwt(jwt -> jwt.subject(VIEWER_UUID).claim("preferred_username", "viewer"));
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_PS_ADMIN"))
                .jwt(jwt -> jwt.subject(ADMIN_UUID).claim("preferred_username", "admin"));
    }

    @Test
    void createCompany_withAnalystRole_succeeds() throws Exception {
        var request = new CompanyCreateRequest(
                "Acme Corp Pvt Ltd", "https://acme.com", "+919876543210",
                "info@acme.com", "Mumbai", "Maharashtra", "West",
                "Manufacturing", "MEDIUM", null, "MANUAL_ENTRY", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/v1/companies")
                        .with(analystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.canonical_name", is("Acme Corp Pvt Ltd")))
                .andExpect(jsonPath("$.normalized_name", notNullValue()))
                .andExpect(jsonPath("$.primary_phone_normalized", is("9876543210")))
                .andExpect(jsonPath("$.website_domain", is("acme.com")))
                .andExpect(jsonPath("$.pipeline_state", is("IMPORTED")))
                .andExpect(jsonPath("$.verification_status", is("UNVERIFIED")))
                .andExpect(jsonPath("$.completeness_score", greaterThanOrEqualTo(0)));
    }

    @Test
    void createCompany_withViewerRole_returns403() throws Exception {
        var request = new CompanyCreateRequest(
                "Viewer Corp", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/v1/companies")
                        .with(viewerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCompany_missingName_returns400() throws Exception {
        var request = new CompanyCreateRequest(
                "", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/v1/companies")
                        .with(analystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void getCompany_exists_returnsDetails() throws Exception {
        String id = createTestCompany("Get Test Corp");

        mockMvc.perform(get("/api/v1/companies/" + id)
                        .with(analystJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canonical_name", is("Get Test Corp")));
    }

    @Test
    void getCompany_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/companies/" + UUID.randomUUID())
                        .with(analystJwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listCompanies_withPagination_works() throws Exception {
        createTestCompany("List Corp A");
        createTestCompany("List Corp B");

        mockMvc.perform(get("/api/v1/companies")
                        .with(analystJwt())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.total_elements", greaterThanOrEqualTo(2)));
    }

    @Test
    void listCompanies_withSearch_filtersResults() throws Exception {
        createTestCompany("UniqueSearchName XYZ");

        mockMvc.perform(get("/api/v1/companies")
                        .with(analystJwt())
                        .param("q", "UniqueSearchName"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void updateCompany_changesFields() throws Exception {
        String id = createTestCompany("Update Target Corp");

        var updateRequest = new CompanyUpdateRequest(
                null, null, null, "newemail@test.com", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        mockMvc.perform(patch("/api/v1/companies/" + id)
                        .with(analystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("newemail@test.com")));
    }

    @Test
    void updateCompany_coreFieldChange_invalidatesVerification() throws Exception {
        String id = createTestCompany("Verify Then Update Corp");

        // verify first
        mockMvc.perform(post("/api/v1/companies/" + id + "/verify")
                        .with(analystJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verification_status", is("VERIFIED")));

        // update core field
        var updateRequest = new CompanyUpdateRequest(
                "New Name Corp", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        mockMvc.perform(patch("/api/v1/companies/" + id)
                        .with(analystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verification_status", is("INVALIDATED")));
    }

    @Test
    void verifyCompany_setsVerificationFields() throws Exception {
        String id = createTestCompany("Verify Me Corp");

        mockMvc.perform(post("/api/v1/companies/" + id + "/verify")
                        .with(analystJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verification_status", is("VERIFIED")))
                .andExpect(jsonPath("$.verified_by", is(ANALYST_UUID)))
                .andExpect(jsonPath("$.verified_at", notNullValue()));
    }

    @Test
    void verifyCompany_withViewerRole_returns403() throws Exception {
        String id = createTestCompany("Viewer Verify Corp");

        mockMvc.perform(post("/api/v1/companies/" + id + "/verify")
                        .with(viewerJwt()))
                .andExpect(status().isForbidden());
    }

    // ---- NIC + pincode combination (docs 21 §4.2 / Companies-page NIC bug fix) ----

    @Test
    void listCompanies_pincodeAndNic_returnsCompaniesMatchingBoth() throws Exception {
        NicCode parent = saveNic("88", "Pincode+NIC parent", (short) 1, null);
        NicCode child = saveNic("8801", "Pincode+NIC child", (short) 2, parent);

        String companyId = createCompanyWithPincode("Pincode Nic Match Corp", "700011");
        attachNic(companyId, child.getId(), true);

        mockMvc.perform(get("/api/v1/companies")
                        .with(analystJwt())
                        .param("pincode", "700011")
                        .param("nic_parent_id", parent.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(companyId)));
    }

    @Test
    void listCompanies_nicFilter_matchesCompanyOnSecondaryNic() throws Exception {
        NicCode parent = saveNic("89", "Secondary-match parent", (short) 1, null);
        NicCode child = saveNic("8901", "Secondary-match child", (short) 2, parent);
        NicCode unrelated = saveNic("90", "Unrelated primary", (short) 1, null);

        String companyId = createTestCompany("Secondary Nic Match Corp");
        attachNic(companyId, unrelated.getId(), true); // first attach -> primary
        attachNic(companyId, child.getId(), false);    // second attach -> secondary

        mockMvc.perform(get("/api/v1/companies")
                        .with(analystJwt())
                        .param("nic_parent_id", parent.getId().toString())
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(companyId)));
    }

    @Test
    void listCompanies_multipleNicParentIds_orsAcrossSelection() throws Exception {
        NicCode nicA = saveNic("91", "OR-select A", (short) 1, null);
        NicCode nicB = saveNic("92", "OR-select B", (short) 1, null);

        String companyA = createTestCompany("OR Match A Corp");
        attachNic(companyA, nicA.getId(), true);
        String companyB = createTestCompany("OR Match B Corp");
        attachNic(companyB, nicB.getId(), true);

        mockMvc.perform(get("/api/v1/companies")
                        .with(analystJwt())
                        .param("nic_parent_ids", nicA.getId().toString(), nicB.getId().toString())
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(companyA)))
                .andExpect(jsonPath("$.content[*].id", hasItem(companyB)));
    }

    @Test
    void listCompanies_configuredNicDefault_intersectsWithPageSelection() throws Exception {
        NicCode root = saveNic("93", "Intersect root", (short) 1, null);
        NicCode childA = saveNic("9301", "Intersect child A", (short) 2, root);
        NicCode childB = saveNic("9302", "Intersect child B", (short) 2, root);

        // Matches BOTH the configured default (root's subtree) and the
        // page's selection (childA) -> must be returned.
        String companyInBoth = createTestCompany("Intersect Match Corp");
        attachNic(companyInBoth, childA.getId(), true);

        // Matches the configured default (also under root's subtree) but NOT
        // the page's narrower selection (childA) -> must be excluded, proving
        // the combination is a real AND/intersection, not "config wins" or
        // "page wins".
        String companyConfigOnly = createTestCompany("Intersect Config-Only Corp");
        attachNic(companyConfigOnly, childB.getId(), true);

        // Isolate this scenario from the seeded starter defaults
        // (V16__company_default_filter_settings.sql ships employee_min=10,
        // gst_present=true, has_nic_primary=true as active) — none of the
        // test companies here set employee_count/gst_number, so if those
        // stayed active they'd zero out the result independently of NIC and
        // this test would pass or fail for the wrong reason. Turn every
        // other active default off for the duration, then restore exactly
        // what was there.
        List<CompanyDefaultFilterResponse> others = listAllDefaults().stream()
                .filter(CompanyDefaultFilterResponse::active)
                .toList();
        for (var d : others) setDefaultActive(d, false);

        String defaultId = createNicDefault(root.getId());
        try {
            mockMvc.perform(get("/api/v1/companies")
                            .with(analystJwt())
                            .param("apply_defaults", "true")
                            .param("nic_parent_ids", childA.getId().toString())
                            .param("size", "200"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].id", hasItem(companyInBoth)))
                    .andExpect(jsonPath("$.content[*].id", not(hasItem(companyConfigOnly))));
        } finally {
            deleteCompanyDefault(defaultId);
            for (var d : others) setDefaultActive(d, true);
        }
    }

    private List<CompanyDefaultFilterResponse> listAllDefaults() throws Exception {
        String response = mockMvc.perform(get("/api/v1/company-default-filters")
                        .with(adminJwt())
                        .param("include_inactive", "true"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return List.of(objectMapper.readValue(response, CompanyDefaultFilterResponse[].class));
    }

    private void setDefaultActive(CompanyDefaultFilterResponse row, boolean active) throws Exception {
        var req = new CompanyDefaultFilterUpsertRequest(
                row.filterKey(), row.label(), row.operator(), row.value(), active, row.sortOrder());
        mockMvc.perform(patch("/api/v1/company-default-filters/" + row.id())
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    private NicCode saveNic(String code, String description, short level, NicCode parent) {
        return nicCodeRepository.save(NicCode.builder()
                .code(code)
                .description(description)
                .industryType("Manufacturing")
                .level(level)
                .parent(parent)
                .isPrimary(false)
                .active(true)
                .build());
    }

    private void attachNic(String companyId, UUID nicCodeId, boolean primary) throws Exception {
        var request = new AttachNicCodeRequest(nicCodeId, null, null, primary);
        mockMvc.perform(post("/api/v1/companies/" + companyId + "/nic-codes")
                        .with(analystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private String createNicDefault(UUID nicParentId) throws Exception {
        var request = new CompanyDefaultFilterUpsertRequest(
                "nic_parent_id", "Test NIC default", "eq",
                "\"" + nicParentId + "\"", true, (short) 999);
        String response = mockMvc.perform(post("/api/v1/company-default-filters")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private void deleteCompanyDefault(String id) throws Exception {
        mockMvc.perform(delete("/api/v1/company-default-filters/" + id)
                        .with(adminJwt()))
                .andExpect(status().isNoContent());
    }

    private String createCompanyWithPincode(String name, String pincode) throws Exception {
        var request = new CompanyCreateRequest(
                name, null, null, null, "TestCity", "TestState", null, null, null, null, null,
                pincode, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        String response = mockMvc.perform(post("/api/v1/companies")
                        .with(analystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }

    private String createTestCompany(String name) throws Exception {
        var request = new CompanyCreateRequest(
                name, null, null, null, "TestCity", "TestState", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        String response = mockMvc.perform(post("/api/v1/companies")
                        .with(analystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }
}
