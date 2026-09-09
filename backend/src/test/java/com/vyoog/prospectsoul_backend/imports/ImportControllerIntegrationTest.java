package com.vyoog.prospectsoul_backend.imports;

import tools.jackson.databind.ObjectMapper;
import com.vyoog.prospectsoul_backend.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ImportControllerIntegrationTest {

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
    void uploadCsv_withAnalystRole_succeeds() throws Exception {
        String csvContent = "Company Name,Phone,Email,City,State\n" +
                "Test Corp,9876543210,test@corp.com,Mumbai,Maharashtra\n" +
                "Another Corp,9876543211,another@corp.com,Delhi,Delhi\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", csvContent.getBytes());

        mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("source", "EXCEL_CSV")
                        .with(analystJwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.file_name", is("test.csv")))
                .andExpect(jsonPath("$.file_type", is("CSV")))
                .andExpect(jsonPath("$.total_rows", is(2)))
                .andExpect(jsonPath("$.status", is("MAPPING")));
    }

    @Test
    void uploadCsv_withViewerRole_returns403() throws Exception {
        String csvContent = "Company Name\nTest\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", csvContent.getBytes());

        mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("source", "EXCEL_CSV")
                        .with(viewerJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadUnsupportedFile_returns422() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "data".getBytes());

        mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("source", "EXCEL_CSV")
                        .with(analystJwt()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void fullImportWorkflow_csv() throws Exception {
        // 1. Upload
        String csvContent = "Company Name,Phone Number,Email,Website,City,State,Industry\n" +
                "Workflow Corp,9876543299,wf@corp.com,www.workflow.com,Pune,Maharashtra,IT\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "workflow.csv", "text/csv", csvContent.getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("source", "EXCEL_CSV")
                        .with(analystJwt()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String batchId = objectMapper.readTree(uploadResponse).get("id").asText();

        // 2. Get mapping suggestions
        mockMvc.perform(get("/api/v1/imports/" + batchId + "/mappings/suggest")
                        .with(analystJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detected_headers", hasSize(7)))
                .andExpect(jsonPath("$.suggestions", hasSize(greaterThanOrEqualTo(7))))
                .andExpect(jsonPath("$.available_target_fields", hasSize(greaterThanOrEqualTo(1))));

        // 3. Confirm mappings
        String mappingBody = """
                {"mappings": [
                    {"source_column": "Company Name", "target_field": "canonical_name"},
                    {"source_column": "Phone Number", "target_field": "primary_phone_normalized"},
                    {"source_column": "Email", "target_field": "email"},
                    {"source_column": "Website", "target_field": "website_domain"},
                    {"source_column": "City", "target_field": "city"},
                    {"source_column": "State", "target_field": "state"},
                    {"source_column": "Industry", "target_field": "industry"}
                ]}""";

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/imports/" + batchId + "/mappings/confirm")
                        .with(analystJwt())
                        .contentType("application/json")
                        .content(mappingBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PREVIEWING")));

        // 4. Preview
        mockMvc.perform(get("/api/v1/imports/" + batchId + "/preview")
                        .with(analystJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_rows", is(1)))
                .andExpect(jsonPath("$.preview_rows", hasSize(1)));

        // 5. Get rows
        mockMvc.perform(get("/api/v1/imports/" + batchId + "/rows")
                        .with(analystJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].row_number", is(1)));

        // 6. Get batch status
        mockMvc.perform(get("/api/v1/imports/" + batchId)
                        .with(analystJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(batchId)));
    }

    @Test
    void listImports_returnsPagedResults() throws Exception {
        mockMvc.perform(get("/api/v1/imports")
                        .with(analystJwt())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page", is(0)));
    }
}
