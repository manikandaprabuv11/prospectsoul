package com.vyoog.prospectsoul_backend.company;

import java.util.UUID;

import tools.jackson.databind.ObjectMapper;
import com.vyoog.prospectsoul_backend.TestcontainersConfiguration;
import com.vyoog.prospectsoul_backend.company.dto.request.CompanyCreateRequest;
import com.vyoog.prospectsoul_backend.company.dto.request.CompanyUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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

    private static final String ANALYST_UUID = "a1000000-0000-0000-0000-000000000001";
    private static final String VIEWER_UUID = "d4000000-0000-0000-0000-000000000001";

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
