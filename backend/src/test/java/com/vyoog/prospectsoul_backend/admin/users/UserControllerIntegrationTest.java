package com.vyoog.prospectsoul_backend.admin.users;

import com.vyoog.prospectsoul_backend.TestcontainersConfiguration;
import com.vyoog.prospectsoul_backend.common.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private Keycloak keycloak;

    private static final String ADMIN_UUID = "c3000000-0000-0000-0000-000000000001";
    private static final String ANALYST_UUID = "a1000000-0000-0000-0000-000000000001";
    private static final String VIEWER_UUID = "d4000000-0000-0000-0000-000000000001";
    private static final String COO_UUID = "e5000000-0000-0000-0000-000000000001";

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_PS_ADMIN"))
                .jwt(jwt -> jwt.subject(ADMIN_UUID).claim("preferred_username", "admin"));
    }

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

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor cooJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_PS_COO"))
                .jwt(jwt -> jwt.subject(COO_UUID).claim("preferred_username", "coo"));
    }

    // --- Role-based access tests ---

    @Test
    void listUsers_withAdminRole_succeeds() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.total_elements", greaterThanOrEqualTo(7)));
    }

    @Test
    void listUsers_withAnalystRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .with(analystJwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void listUsers_withViewerRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .with(viewerJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_withCooRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .with(cooJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.status", is(401)));
    }

    // --- Seed data verification ---

    @Test
    void seedData_7usersExist() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .with(adminJwt())
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements", greaterThanOrEqualTo(7)));
    }

    @Test
    void seedData_filterByRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .with(adminJwt())
                        .param("role", "PS_ANALYST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements", greaterThanOrEqualTo(2)));

        mockMvc.perform(get("/api/v1/admin/users")
                        .with(adminJwt())
                        .param("role", "PS_VIEWER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements", greaterThanOrEqualTo(2)));
    }

    @Test
    void seedData_getAnalystById() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/" + ANALYST_UUID)
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("analyst")))
                .andExpect(jsonPath("$.full_name", is("Priya Sharma")))
                .andExpect(jsonPath("$.email", is("priya@vyoog.com")))
                .andExpect(jsonPath("$.role", is("PS_ANALYST")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    // --- Search and pagination ---

    @Test
    void listUsers_search_filtersResults() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .with(adminJwt())
                        .param("q", "Priya"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].full_name", containsString("Priya")));
    }

    @Test
    void listUsers_pagination_works() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .with(adminJwt())
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(3)))
                .andExpect(jsonPath("$.total_pages", greaterThanOrEqualTo(2)));
    }

    // --- User not found ---

    @Test
    void getUser_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/" + UUID.randomUUID())
                        .with(adminJwt()))
                .andExpect(status().isNotFound());
    }

    // --- Full CRUD cycle (with Keycloak mock) ---

    @Test
    void adminCrudCycle_createUpdateDeactivateActivate() throws Exception {
        String newUserId = "ff000000-0000-0000-0000-000000000099";
        mockKeycloakForUserCreate(newUserId);

        // CREATE
        String createJson = """
                {
                    "username": "newuser",
                    "full_name": "New User",
                    "email": "newuser@vyoog.com",
                    "password": "Password123!",
                    "role": "PS_VIEWER"
                }
                """;

        String createResponse = mockMvc.perform(post("/api/v1/admin/users")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(newUserId)))
                .andExpect(jsonPath("$.username", is("newuser")))
                .andExpect(jsonPath("$.full_name", is("New User")))
                .andExpect(jsonPath("$.role", is("PS_VIEWER")))
                .andExpect(jsonPath("$.active", is(true)))
                .andReturn().getResponse().getContentAsString();

        // Verify audit row for CREATE
        var auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                "USER", UUID.fromString(newUserId));
        assertThat(auditLogs).isNotEmpty();
        assertThat(auditLogs.getFirst().getAction()).isEqualTo("CREATE");
        assertThat(auditLogs.getFirst().getActor()).isEqualTo(ADMIN_UUID);

        // UPDATE
        mockKeycloakForUserUpdate(newUserId);

        String updateJson = """
                {
                    "full_name": "Updated User",
                    "email": "updated@vyoog.com"
                }
                """;

        mockMvc.perform(patch("/api/v1/admin/users/" + newUserId)
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.full_name", is("Updated User")))
                .andExpect(jsonPath("$.email", is("updated@vyoog.com")));

        // Verify audit row for UPDATE
        var updateAuditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                "USER", UUID.fromString(newUserId));
        assertThat(updateAuditLogs.size()).isGreaterThanOrEqualTo(2);
        assertThat(updateAuditLogs.getFirst().getAction()).isEqualTo("UPDATE");

        // DEACTIVATE
        mockKeycloakForEnableDisable(newUserId);

        mockMvc.perform(post("/api/v1/admin/users/" + newUserId + "/deactivate")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));

        // ACTIVATE
        mockMvc.perform(post("/api/v1/admin/users/" + newUserId + "/activate")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(true)));
    }

    // --- Validation ---

    @Test
    void createUser_invalidRole_returns422() throws Exception {
        mockKeycloakForUserCreate("ff000000-0000-0000-0000-000000000088");

        String json = """
                {
                    "username": "badrole",
                    "full_name": "Bad Role User",
                    "email": "badrole@vyoog.com",
                    "password": "Password123!",
                    "role": "PS_INVALID"
                }
                """;

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createUser_missingFields_returns400() throws Exception {
        String json = """
                {
                    "username": "",
                    "full_name": "",
                    "email": "not-email",
                    "password": "short",
                    "role": ""
                }
                """;

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    // --- Problem+json format ---

    @Test
    void unauthorized_returnsProblemJson() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type", is("about:blank")))
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void forbidden_returnsProblemJson() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .with(viewerJwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type", is("about:blank")))
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.detail").exists());
    }

    // --- Keycloak mocking helpers ---

    private void mockKeycloakForUserCreate(String userId) {
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        RolesResource rolesResource = mock(RolesResource.class);
        RoleResource roleResource = mock(RoleResource.class);
        UserResource userResource = mock(UserResource.class);
        RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
        RoleScopeResource roleScopeResource = mock(RoleScopeResource.class);

        when(keycloak.realm("vyoog")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(realmResource.roles()).thenReturn(rolesResource);

        Response createResponse = mock(Response.class);
        when(createResponse.getStatus()).thenReturn(201);
        when(createResponse.getHeaderString("Location"))
                .thenReturn("http://localhost:8081/admin/realms/vyoog/users/" + userId);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(createResponse);

        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setName("PS_VIEWER");
        when(rolesResource.get(anyString())).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(roleRep);

        when(usersResource.get(userId)).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
    }

    private void mockKeycloakForUserUpdate(String userId) {
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);

        when(keycloak.realm("vyoog")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);

        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername("newuser");
        when(userResource.toRepresentation()).thenReturn(userRep);
    }

    private void mockKeycloakForEnableDisable(String userId) {
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);

        when(keycloak.realm("vyoog")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);

        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername("newuser");
        when(userResource.toRepresentation()).thenReturn(userRep);
    }
}
