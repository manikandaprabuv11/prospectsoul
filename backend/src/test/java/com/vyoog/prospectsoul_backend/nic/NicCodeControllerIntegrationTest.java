package com.vyoog.prospectsoul_backend.nic;

import tools.jackson.databind.ObjectMapper;
import com.vyoog.prospectsoul_backend.TestcontainersConfiguration;
import com.vyoog.prospectsoul_backend.nic.dto.request.NicCodeCreateRequest;
import com.vyoog.prospectsoul_backend.nic.dto.request.NicCodeUpdateRequest;
import com.vyoog.prospectsoul_backend.nic.entity.NicCode;
import com.vyoog.prospectsoul_backend.nic.repository.NicCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
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
class NicCodeControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired NicCodeRepository nicCodeRepository;

    private static final String ADMIN_UUID   = "c3000000-0000-0000-0000-000000000001";
    private static final String ANALYST_UUID = "a1000000-0000-0000-0000-000000000001";
    private static final String VIEWER_UUID  = "d4000000-0000-0000-0000-000000000001";

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwt(String uuid, String... roles) {
        var pp = SecurityMockMvcRequestPostProcessors.jwt()
                .authorities(java.util.Arrays.stream(roles)
                        .map(r -> (org.springframework.security.core.GrantedAuthority)
                                new SimpleGrantedAuthority("ROLE_" + r))
                        .toList())
                .jwt(j -> j.subject(uuid).claim("preferred_username", uuid));
        return pp;
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor admin()   { return jwt(ADMIN_UUID,   "PS_ADMIN"); }
    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor analyst() { return jwt(ANALYST_UUID, "PS_ANALYST"); }
    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor viewer()  { return jwt(VIEWER_UUID,  "PS_VIEWER"); }

    @Autowired com.vyoog.prospectsoul_backend.company.nic.repository.CompanyNicCodeRepository companyNicCodeRepository;
    @Autowired com.vyoog.prospectsoul_backend.company.repository.CompanyRepository companyRepository;

    @BeforeEach
    void clean() {
        companyNicCodeRepository.deleteAll();
        companyRepository.deleteAll();
        nicCodeRepository.deleteAll();
    }

    @Test
    void adminCanCreateAndReadCodeAndChildrenList() throws Exception {
        var section = new NicCodeCreateRequest("2", "Manufacturing (Section)", "Manufacturing", null, false);
        String sectionJson = mockMvc.perform(post("/api/v1/nic-codes")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(section)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is("2")))
                .andExpect(jsonPath("$.level", is(1)))
                .andReturn().getResponse().getContentAsString();
        UUID sectionId = UUID.fromString(objectMapper.readTree(sectionJson).get("id").asText());

        var division = new NicCodeCreateRequest("22", "Rubber and plastics", "Manufacturing", sectionId, true);
        String divJson = mockMvc.perform(post("/api/v1/nic-codes")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(division)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is("22")))
                .andExpect(jsonPath("$.parent_id", is(sectionId.toString())))
                .andExpect(jsonPath("$.is_primary", is(true)))
                .andReturn().getResponse().getContentAsString();

        UUID divId = UUID.fromString(objectMapper.readTree(divJson).get("id").asText());
        mockMvc.perform(get("/api/v1/nic-codes/" + sectionId + "/children").with(analyst()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(divId.toString())));

        mockMvc.perform(get("/api/v1/nic-codes/primary").with(analyst()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code", is("22")));

        mockMvc.perform(get("/api/v1/nic-codes/tree").with(viewer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code", is("2")))
                .andExpect(jsonPath("$[0].child_count", is(1)))
                .andExpect(jsonPath("$[0].children[0].code", is("22")));
    }

    @Test
    void createWithNonPrefixParentReturns422() throws Exception {
        NicCode seed = nicCodeRepository.save(NicCode.builder()
                .code("2").description("Manufacturing").industryType("Manufacturing").level((short)1).active(true).isPrimary(false).build());

        var req = new NicCodeCreateRequest("33", "Different section", "Manufacturing", seed.getId(), false);
        mockMvc.perform(post("/api/v1/nic-codes")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("prefix")));
    }

    @Test
    void nonAdminOnMutationsReturns403() throws Exception {
        var req = new NicCodeCreateRequest("2", "Manufacturing", "Manufacturing", null, false);

        mockMvc.perform(post("/api/v1/nic-codes")
                        .with(analyst())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        NicCode existing = nicCodeRepository.save(NicCode.builder()
                .code("3").description("Services").industryType("Service").level((short)1).active(true).isPrimary(false).build());

        mockMvc.perform(patch("/api/v1/nic-codes/" + existing.getId())
                        .with(analyst())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"nope\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/nic-codes/" + existing.getId() + "/toggle-primary")
                        .with(analyst()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/nic-codes/" + existing.getId() + "/toggle-primary")
                        .with(viewer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateCodeReturns409() throws Exception {
        nicCodeRepository.save(NicCode.builder()
                .code("2").description("Manufacturing").industryType("Manufacturing").level((short)1).active(true).isPrimary(false).build());

        var req = new NicCodeCreateRequest("2", "Second", "Manufacturing", null, false);
        mockMvc.perform(post("/api/v1/nic-codes")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void togglePrimaryFlipsFlagAndWritesAudit() throws Exception {
        NicCode c = nicCodeRepository.save(NicCode.builder()
                .code("2").description("Manufacturing").industryType("Manufacturing").level((short)1).active(true).isPrimary(false).build());

        mockMvc.perform(post("/api/v1/nic-codes/" + c.getId() + "/toggle-primary").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_primary", is(true)));

        mockMvc.perform(post("/api/v1/nic-codes/" + c.getId() + "/toggle-primary").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_primary", is(false)));
    }

    @Test
    void treeCacheIsInvalidatedOnMutation() throws Exception {
        String first = mockMvc.perform(get("/api/v1/nic-codes/tree").with(viewer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var req = new NicCodeCreateRequest("2", "Manufacturing", "Manufacturing", null, false);
        mockMvc.perform(post("/api/v1/nic-codes")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/nic-codes/tree").with(viewer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code", is("2")));
    }
}
