package com.vyoog.prospectsoul_backend.company.phone;

import java.util.UUID;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.vyoog.prospectsoul_backend.TestcontainersConfiguration;
import com.vyoog.prospectsoul_backend.company.dto.request.CompanyCreateRequest;
import com.vyoog.prospectsoul_backend.company.phone.dto.request.CompanyPhoneRequest;
import com.vyoog.prospectsoul_backend.company.phone.dto.request.ConfidenceOverrideRequest;
import com.vyoog.prospectsoul_backend.company.phone.entity.ConfidenceLevel;
import com.vyoog.prospectsoul_backend.company.phone.entity.NumberSourceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
class CompanyPhoneControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static final String ANALYST_UUID = "a1000000-0000-0000-0000-000000000001";
    private static final String VIEWER_UUID  = "d4000000-0000-0000-0000-000000000001";

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor analystJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_PS_ANALYST"))
                .jwt(jwt -> jwt.subject(ANALYST_UUID).claim("preferred_username", "analyst"));
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor viewerJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_PS_VIEWER"))
                .jwt(jwt -> jwt.subject(VIEWER_UUID).claim("preferred_username", "viewer"));
    }

    private String createCompany(String suffix) throws Exception {
        var request = new CompanyCreateRequest(
                "PhoneTest " + suffix, null, null, null, "Chennai", "Tamil Nadu",
                null, "Manufacturing", null, null, "MANUAL_ENTRY",
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        MvcResult result = mockMvc.perform(post("/api/v1/companies")
                        .with(analystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asText();
    }

    private String addPhone(String companyId, String number, NumberSourceType source,
                            String designation, boolean primary) throws Exception {
        var request = new CompanyPhoneRequest(null, number, source, null, null, designation, primary);

        MvcResult result = mockMvc.perform(post("/api/v1/companies/" + companyId + "/phones")
                        .with(analystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asText();
    }

    // --- TC-1: Add phone, confidence computed from source ---

    @Test
    void addPhone_businessCard_confidenceHigh() throws Exception {
        String companyId = createCompany(UUID.randomUUID().toString().substring(0, 8));

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/phones")
                        .with(analystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CompanyPhoneRequest(null, "9843012345", NumberSourceType.BUSINESS_CARD,
                                        null, null, null, true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.number_normalized", is("9843012345")))
                .andExpect(jsonPath("$.phone_type", is("MOBILE")))
                .andExpect(jsonPath("$.confidence", is("HIGH")))
                .andExpect(jsonPath("$.confidence_mode", is("AUTO")))
                .andExpect(jsonPath("$.is_primary", is(true)));
    }

    @Test
    void addPhone_indiamart_confidenceLow() throws Exception {
        String companyId = createCompany(UUID.randomUUID().toString().substring(0, 8));

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/phones")
                        .with(analystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CompanyPhoneRequest(null, "9944012345", NumberSourceType.INDIAMART,
                                        null, null, null, false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.confidence", is("LOW")));
    }

    // --- TC-2: Designation override upgrades confidence ---

    @Test
    void addPhone_websiteWithMdDesignation_confidenceHigh() throws Exception {
        String companyId = createCompany(UUID.randomUUID().toString().substring(0, 8));

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/phones")
                        .with(analystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CompanyPhoneRequest(null, "9843099999", NumberSourceType.WEBSITE,
                                        null, null, "MD", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.confidence", is("HIGH")))
                .andExpect(jsonPath("$.designation_override", is("MD")));
    }

    @Test
    void addPhone_indiamartWithMd_confidenceMedium() throws Exception {
        String companyId = createCompany(UUID.randomUUID().toString().substring(0, 8));

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/phones")
                        .with(analystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CompanyPhoneRequest(null, "9843088888", NumberSourceType.INDIAMART,
                                        null, null, "MD", false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.confidence", is("MEDIUM")));
    }

    // --- TC-3: Role enforcement ---

    @Test
    void addPhone_viewerRole_returns403() throws Exception {
        String companyId = createCompany(UUID.randomUUID().toString().substring(0, 8));

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/phones")
                        .with(viewerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CompanyPhoneRequest(null, "9843012345", NumberSourceType.MANUAL_ENTRY,
                                        null, null, null, false))))
                .andExpect(status().isForbidden());
    }

    // --- TC-4: List phones ---

    @Test
    void listPhones_returnsAddedPhone() throws Exception {
        String companyId = createCompany(UUID.randomUUID().toString().substring(0, 8));
        addPhone(companyId, "9843055555", NumberSourceType.FIELD_VISIT, null, true);

        mockMvc.perform(get("/api/v1/companies/" + companyId + "/phones")
                        .with(analystJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].number_normalized", is("9843055555")))
                .andExpect(jsonPath("$[0].is_primary", is(true)));
    }

    // --- TC-5: Duplicate phone on same company returns 409 ---

    @Test
    void addPhone_duplicateOnSameCompany_returns409() throws Exception {
        String companyId = createCompany(UUID.randomUUID().toString().substring(0, 8));
        addPhone(companyId, "9843077777", NumberSourceType.BUSINESS_CARD, null, true);

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/phones")
                        .with(analystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CompanyPhoneRequest(null, "9843077777", NumberSourceType.WEBSITE,
                                        null, null, null, false))))
                .andExpect(status().isConflict());
    }

    // --- TC-6: Delete phone ---

    @Test
    void deletePhone_succeeds() throws Exception {
        String companyId = createCompany(UUID.randomUUID().toString().substring(0, 8));
        String phoneId = addPhone(companyId, "9843066666", NumberSourceType.MANUAL_ENTRY, null, true);

        mockMvc.perform(delete("/api/v1/company-phones/" + phoneId)
                        .with(analystJwt()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/companies/" + companyId + "/phones")
                        .with(analystJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- TC-7: Override confidence sets MANUAL mode ---

    @Test
    void overrideConfidence_setsManualMode() throws Exception {
        String companyId = createCompany(UUID.randomUUID().toString().substring(0, 8));
        String phoneId = addPhone(companyId, "9843044444", NumberSourceType.INDIAMART, null, true);

        mockMvc.perform(post("/api/v1/company-phones/" + phoneId + "/override-confidence")
                        .with(analystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ConfidenceOverrideRequest(ConfidenceLevel.HIGH,
                                        "Verified by field visit on site"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confidence", is("HIGH")))
                .andExpect(jsonPath("$.confidence_mode", is("MANUAL")))
                .andExpect(jsonPath("$.override_reason", is("Verified by field visit on site")));
    }

    @Test
    void overrideConfidence_shortReason_returns400() throws Exception {
        String companyId = createCompany(UUID.randomUUID().toString().substring(0, 8));
        String phoneId = addPhone(companyId, "9843033333", NumberSourceType.MANUAL_ENTRY, null, true);

        mockMvc.perform(post("/api/v1/company-phones/" + phoneId + "/override-confidence")
                        .with(analystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ConfidenceOverrideRequest(ConfidenceLevel.HIGH, "short"))))
                .andExpect(status().isBadRequest());
    }

    // --- TC-8: Update phone, change primary ---

    @Test
    void updatePhone_changePrimary() throws Exception {
        String companyId = createCompany(UUID.randomUUID().toString().substring(0, 8));
        String phone1Id = addPhone(companyId, "9843022222", NumberSourceType.MANUAL_ENTRY, null, true);
        String phone2Id = addPhone(companyId, "9843011111", NumberSourceType.BUSINESS_CARD, null, false);

        mockMvc.perform(patch("/api/v1/company-phones/" + phone2Id)
                        .with(analystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CompanyPhoneRequest(null, "9843011111", NumberSourceType.BUSINESS_CARD,
                                        null, null, null, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_primary", is(true)));

        mockMvc.perform(get("/api/v1/companies/" + companyId + "/phones")
                        .with(analystJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // --- TC-9: GST validation on company create ---

    @Test
    void createCompany_validGst_succeeds() throws Exception {
        var request = new CompanyCreateRequest(
                "GSTTest " + UUID.randomUUID().toString().substring(0, 8),
                null, null, null, null, null, null, null, null, null, "MANUAL_ENTRY",
                null, null, null, null, null, null, "27AAPFU0939F1ZV",
                null, null, null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/v1/companies")
                        .with(analystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gst_number", is("27AAPFU0939F1ZV")));
    }

    @Test
    void createCompany_invalidGstCheckDigit_returns422() throws Exception {
        var request = new CompanyCreateRequest(
                "GSTBad " + UUID.randomUUID().toString().substring(0, 8),
                null, null, null, null, null, null, null, null, null, "MANUAL_ENTRY",
                null, null, null, null, null, null, "27AAPFU0939F1ZA",
                null, null, null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/v1/companies")
                        .with(analystJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    // --- TC-10: Companies list confidence filter ---

    @Test
    void listCompanies_confidenceFilter() throws Exception {
        String highCompanyId = createCompany("confH-" + UUID.randomUUID().toString().substring(0, 6));
        addPhone(highCompanyId, "984300" + (int)(Math.random() * 9000 + 1000), NumberSourceType.BUSINESS_CARD, null, true);

        String lowCompanyId = createCompany("confL-" + UUID.randomUUID().toString().substring(0, 6));
        addPhone(lowCompanyId, "984301" + (int)(Math.random() * 9000 + 1000), NumberSourceType.INDIAMART, null, true);

        mockMvc.perform(get("/api/v1/companies")
                        .param("confidence", "HIGH")
                        .param("apply_defaults", "false")
                        .with(analystJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
