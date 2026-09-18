package com.vyoog.prospectsoul_backend.verification;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.TestcontainersConfiguration;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.repository.CompanyRepository;
import com.vyoog.prospectsoul_backend.verification.dto.request.StartVerificationRequest;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationItemStatus;
import com.vyoog.prospectsoul_backend.verification.repository.VerificationBatchItemRepository;
import com.vyoog.prospectsoul_backend.verification.worker.VerificationWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The HTTP contract: status codes, RFC-7807 problem bodies, snake_case JSON and
 * the role matrix of docs/dev_docs/13 §12.
 *
 * <p>Authorization is asserted here, at the boundary the backend actually
 * enforces. The frontend hiding a button is never the control.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, VerificationTestConfiguration.class})
class VerificationControllerIntegrationTest {

    private static final String ANALYST_UUID = "a1000000-0000-0000-0000-000000000001";
    private static final String SALES_LEAD_UUID = "b2000000-0000-0000-0000-000000000001";
    private static final String ADMIN_UUID = "c3000000-0000-0000-0000-000000000001";
    private static final String VIEWER_UUID = "d4000000-0000-0000-0000-000000000001";
    private static final String COO_UUID = "e5000000-0000-0000-0000-000000000001";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private VerificationBatchItemRepository itemRepository;
    @Autowired private VerificationWorker worker;
    @Autowired private StubPhoneVerificationProvider provider;

    @BeforeEach
    void resetProvider() {
        provider.reset();
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwt(
            String role, String subject, String username) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_" + role))
                .jwt(token -> token.subject(subject).claim("preferred_username", username));
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor analyst() {
        return jwt("PS_ANALYST", ANALYST_UUID, "analyst");
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor salesLead() {
        return jwt("PS_SALES_LEAD", SALES_LEAD_UUID, "saleslead");
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor admin() {
        return jwt("PS_ADMIN", ADMIN_UUID, "admin");
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor viewer() {
        return jwt("PS_VIEWER", VIEWER_UUID, "viewer");
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor coo() {
        return jwt("PS_COO", COO_UUID, "coo");
    }

    // ------------------------------------------------------------------
    // POST /api/v1/verifications
    // ------------------------------------------------------------------

    @Test
    void start_returns202AndSnakeCaseBody() throws Exception {
        Company company = company("Controller Start Corp", "9811100001");

        mockMvc.perform(post("/api/v1/verifications")
                        .with(analyst())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new StartVerificationRequest(List.of(company.getId()), null, null, null))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status", is("QUEUED")))
                .andExpect(jsonPath("$.total_count", is(1)))
                .andExpect(jsonPath("$.queued_count", is(1)))
                .andExpect(jsonPath("$.processing_count", is(0)))
                .andExpect(jsonPath("$.verified_count", is(0)))
                .andExpect(jsonPath("$.failed_count", is(0)))
                .andExpect(jsonPath("$.skipped_count", is(0)))
                .andExpect(jsonPath("$.progress_percent", is(0)))
                .andExpect(jsonPath("$.requested_by", is(ANALYST_UUID)))
                .andExpect(jsonPath("$.requested_by_name", is("Priya Sharma")));
    }

    @Test
    void start_acceptsSnakeCaseRequestBody() throws Exception {
        Company company = company("Snake Case Corp", "9811100002");

        String body = objectMapper.writeValueAsString(Map.of(
                "company_ids", List.of(company.getId().toString()),
                "added_by", ANALYST_UUID,
                "date_from", "2020-01-01",
                "date_to", "2099-12-31"));

        mockMvc.perform(post("/api/v1/verifications")
                        .with(analyst())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.filter_added_by", is(ANALYST_UUID)))
                .andExpect(jsonPath("$.filter_date_from", is("2020-01-01")))
                .andExpect(jsonPath("$.filter_date_to", is("2099-12-31")));
    }

    @Test
    void start_salesLeadAndAdminAreAlsoAllowed() throws Exception {
        Company forLead = company("Lead Corp", "9811100010");
        Company forAdmin = company("Admin Corp", "9811100011");

        mockMvc.perform(post("/api/v1/verifications")
                        .with(salesLead())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new StartVerificationRequest(List.of(forLead.getId()), null, null, null))))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v1/verifications")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new StartVerificationRequest(List.of(forAdmin.getId()), null, null, null))))
                .andExpect(status().isAccepted());
    }

    @Test
    void start_asViewer_is403() throws Exception {
        Company company = company("Viewer Denied Corp", "9811100020");

        mockMvc.perform(post("/api/v1/verifications")
                        .with(viewer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new StartVerificationRequest(List.of(company.getId()), null, null, null))))
                .andExpect(status().isForbidden());

        // The denial is real, not cosmetic: no queue row was created.
        assertThat(itemRepository.findCompanyIdsInFlight(List.of(company.getId()))).isEmpty();
    }

    @Test
    void start_asCoo_is403() throws Exception {
        Company company = company("COO Denied Corp", "9811100021");

        mockMvc.perform(post("/api/v1/verifications")
                        .with(coo())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new StartVerificationRequest(List.of(company.getId()), null, null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void start_withoutAToken_is401() throws Exception {
        Company company = company("Anonymous Denied Corp", "9811100022");

        mockMvc.perform(post("/api/v1/verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new StartVerificationRequest(List.of(company.getId()), null, null, null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void start_withEmptySelection_is400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/verifications")
                        .with(analyst())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_ids\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void start_withInvertedDateRange_is400ProblemJson() throws Exception {
        Company company = company("Bad Range Controller Corp", "9811100030");

        mockMvc.perform(post("/api/v1/verifications")
                        .with(analyst())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "company_ids", List.of(company.getId().toString()),
                                "date_from", "2026-09-10",
                                "date_to", "2026-09-01"))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", is("date_from must not be after date_to")));
    }

    @Test
    void start_withNoEligibleCompany_is422ProblemJson() throws Exception {
        Company noPhone = company("Controller No Phone Corp", null);

        mockMvc.perform(post("/api/v1/verifications")
                        .with(analyst())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new StartVerificationRequest(List.of(noPhone.getId()), null, null, null))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title", is("Business Rule Violation")))
                .andExpect(jsonPath("$.status", is(422)));
    }

    @Test
    void start_forACompanyAlreadyInFlight_is409ProblemJson() throws Exception {
        Company company = company("Controller In Flight Corp", "9811100040");
        String body = objectMapper.writeValueAsString(
                new StartVerificationRequest(List.of(company.getId()), null, null, null));

        mockMvc.perform(post("/api/v1/verifications").with(analyst())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v1/verifications").with(analyst())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title", is("Conflict")));
    }

    @Test
    void start_forAnUnknownCompany_is404ProblemJson() throws Exception {
        mockMvc.perform(post("/api/v1/verifications")
                        .with(analyst())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new StartVerificationRequest(List.of(UUID.randomUUID()), null, null, null))))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title", is("Not Found")));
    }

    // ------------------------------------------------------------------
    // GET endpoints
    // ------------------------------------------------------------------

    @Test
    void active_is204WhenNothingIsRunningAnd200WhileABatchIsOpen() throws Exception {
        drain();

        mockMvc.perform(get("/api/v1/verifications/active").with(analyst()))
                .andExpect(status().isNoContent());

        Company company = company("Active Controller Corp", "9811200001");
        provider.script("+919811200001", StubPhoneVerificationProvider.mobile("+919811200001", "Airtel"));
        mockMvc.perform(post("/api/v1/verifications").with(analyst())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new StartVerificationRequest(List.of(company.getId()), null, null, null))))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/verifications/active").with(analyst()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("QUEUED")))
                .andExpect(jsonPath("$.total_count", is(1)));

        drain();

        mockMvc.perform(get("/api/v1/verifications/active").with(analyst()))
                .andExpect(status().isNoContent());
    }

    @Test
    void everyReadRoleCanReachEveryReadEndpoint() throws Exception {
        Company company = company("Read Role Corp", "9811200010");
        provider.script("+919811200010", StubPhoneVerificationProvider.mobile("+919811200010", "Airtel"));

        String batchId = objectMapper.readTree(mockMvc.perform(post("/api/v1/verifications")
                                .with(analyst())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new StartVerificationRequest(
                                                List.of(company.getId()), null, null, null))))
                        .andReturn().getResponse().getContentAsString())
                .get("id").asString();
        drain();

        for (var role : List.of(analyst(), salesLead(), admin(), viewer(), coo())) {
            mockMvc.perform(get("/api/v1/verifications").with(role)).andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/verifications/" + batchId).with(role)).andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/verifications/" + batchId + "/items").with(role))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/verifications/eligible").with(role)).andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/verifications/companies").with(role)).andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/verifications/added-by-options").with(role))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void readEndpoints_withoutAToken_are401() throws Exception {
        mockMvc.perform(get("/api/v1/verifications")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/verifications/active")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/verifications/eligible")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/verifications/companies")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/verifications/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_usesTheProjectPaginationEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/verifications")
                        .param("page", "0").param("size", "5")
                        .with(analyst()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(5)))
                .andExpect(jsonPath("$.total_elements", notNullValue()))
                .andExpect(jsonPath("$.total_pages", notNullValue()));
    }

    @Test
    void eligible_returnsSnakeCaseRowsAndNeverAVerifiedCompany() throws Exception {
        Company unverified = company("Eligible Controller Corp", "9811200020");
        Company verified = company("Verified Controller Corp", "9811200021");
        verified.setVerificationStatus(Company.VerificationStatus.VERIFIED);
        verified.setVerifiedAt(Instant.now());
        verified.setVerifiedBy(ANALYST_UUID);
        companyRepository.save(verified);

        mockMvc.perform(get("/api/v1/verifications/eligible")
                        .param("q", "Controller Corp")
                        .param("added_by", ANALYST_UUID)
                        .with(analyst()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].canonical_name", notNullValue()))
                .andExpect(jsonPath("$.content[0].added_by_name", notNullValue()))
                .andExpect(jsonPath("$.content[0].phone_usable", notNullValue()))
                .andExpect(jsonPath("$.content[0].in_flight", notNullValue()))
                .andExpect(jsonPath("$.content[?(@.id == '" + verified.getId() + "')]", hasSize(0)))
                .andExpect(jsonPath("$.content[?(@.id == '" + unverified.getId() + "')]", hasSize(1)));
    }

    @Test
    void eligible_withVerifiedStatusFilter_is400() throws Exception {
        mockMvc.perform(get("/api/v1/verifications/eligible")
                        .param("verification_status", "VERIFIED")
                        .with(analyst()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void verifiedCompanies_returnOnlyVerifiedRowsWithProviderDetail() throws Exception {
        Company company = company("Verified Table Controller Corp", "9811200030");
        provider.script("+919811200030", StubPhoneVerificationProvider.mobile("+919811200030", "Jio"));

        mockMvc.perform(post("/api/v1/verifications").with(analyst())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new StartVerificationRequest(List.of(company.getId()), null, null, null))))
                .andExpect(status().isAccepted());
        drain();

        mockMvc.perform(get("/api/v1/verifications/companies")
                        .param("q", "Verified Table Controller")
                        .with(viewer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].canonical_name", is("Verified Table Controller Corp")))
                .andExpect(jsonPath("$.content[0].verified_by_name", is("Priya Sharma")))
                .andExpect(jsonPath("$.content[0].verified_at", notNullValue()))
                .andExpect(jsonPath("$.content[0].added_by_name", is("Priya Sharma")))
                .andExpect(jsonPath("$.content[0].verified_phone", is("+919811200030")))
                .andExpect(jsonPath("$.content[0].line_type", is("mobile")))
                .andExpect(jsonPath("$.content[0].carrier_name", is("Jio")));
    }

    @Test
    void items_exposeFailureReasonsInSnakeCase() throws Exception {
        Company company = company("Items Controller Corp", "9811200040");
        provider.script("+919811200040", StubPhoneVerificationProvider.invalid("+919811200040"));

        String batchId = objectMapper.readTree(mockMvc.perform(post("/api/v1/verifications")
                                .with(analyst())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new StartVerificationRequest(
                                                List.of(company.getId()), null, null, null))))
                        .andReturn().getResponse().getContentAsString())
                .get("id").asString();
        drain();

        mockMvc.perform(get("/api/v1/verifications/" + batchId + "/items")
                        .param("status", "FAILED")
                        .with(analyst()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].company_name", is("Items Controller Corp")))
                .andExpect(jsonPath("$.content[0].failure_code", is("INVALID_NUMBER")))
                .andExpect(jsonPath("$.content[0].failure_message", notNullValue()))
                .andExpect(jsonPath("$.content[0].normalized_phone_number", is("+919811200040")))
                .andExpect(jsonPath("$.content[0].attempt_count", is(1)))
                .andExpect(jsonPath("$.content[0].phone_valid", is(false)));
    }

    @Test
    void batchDetail_forAnUnknownId_is404() throws Exception {
        mockMvc.perform(get("/api/v1/verifications/" + UUID.randomUUID()).with(analyst()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/verifications/" + UUID.randomUUID() + "/items").with(analyst()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownStatusFilter_is400() throws Exception {
        mockMvc.perform(get("/api/v1/verifications").param("status", "NOT_A_STATUS").with(analyst()))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void drain() {
        for (int pass = 0; pass < 200; pass++) {
            if (itemRepository.countByStatus(VerificationItemStatus.QUEUED) == 0
                    && itemRepository.countByStatus(VerificationItemStatus.PROCESSING) == 0) {
                worker.reconcileNonTerminalBatches();
                return;
            }
            worker.runOnce();
        }
        throw new AssertionError("worker did not drain the queue within 200 passes");
    }

    private Company company(String name, String phone) {
        return companyRepository.save(Company.builder()
                .canonicalName(name)
                .normalizedName(name.toLowerCase().replace(" ", ""))
                .primaryPhoneNormalized(phone)
                .city("Coimbatore")
                .state("Tamil Nadu")
                .source("MANUAL_ENTRY")
                .createdBy(ANALYST_UUID)
                .updatedBy(ANALYST_UUID)
                .build());
    }
}
