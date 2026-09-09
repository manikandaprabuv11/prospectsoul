package com.vyoog.prospectsoul_backend.admin.roles;

import com.vyoog.prospectsoul_backend.TestcontainersConfiguration;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RoleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Keycloak keycloak;

    private static final String ADMIN_UUID = "c3000000-0000-0000-0000-000000000001";
    private static final String ANALYST_UUID = "a1000000-0000-0000-0000-000000000001";
    private static final String SALES_LEAD_UUID = "b2000000-0000-0000-0000-000000000001";

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

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor salesLeadJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_PS_SALES_LEAD"))
                .jwt(jwt -> jwt.subject(SALES_LEAD_UUID).claim("preferred_username", "saleslead"));
    }

    // --- Seed data verification: 5 roles ---

    @Test
    void listRoles_returns5SeedRoles() throws Exception {
        mockMvc.perform(get("/api/v1/admin/roles")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[?(@.name == 'PS_ANALYST')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'PS_SALES_LEAD')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'PS_ADMIN')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'PS_VIEWER')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'PS_COO')]").exists());
    }

    @Test
    void getRoleById_analyst_returnsDetail() throws Exception {
        String roleId = "10000000-0000-0000-0000-000000000001";

        mockMvc.perform(get("/api/v1/admin/roles/" + roleId)
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("PS_ANALYST")))
                .andExpect(jsonPath("$.display_name", is("Research Analyst")))
                .andExpect(jsonPath("$.permissions").isArray())
                .andExpect(jsonPath("$.permissions", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.assigned_users").isArray());
    }

    // --- Role-based access ---

    @Test
    void listRoles_withAnalystRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/roles")
                        .with(analystJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listRoles_withSalesLeadRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/roles")
                        .with(salesLeadJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listRoles_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/roles"))
                .andExpect(status().isUnauthorized());
    }

    // --- Update role description ---

    @Test
    void updateRole_description_succeeds() throws Exception {
        String roleId = "10000000-0000-0000-0000-000000000004";

        String json = """
                { "description": "Updated viewer description" }
                """;

        mockMvc.perform(patch("/api/v1/admin/roles/" + roleId)
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", is("Updated viewer description")));
    }

    // --- Assign/Unassign user (with Keycloak mock) ---

    @Test
    void assignUser_andGetAssignedUsers() throws Exception {
        String analystRoleId = "10000000-0000-0000-0000-000000000001";
        String viewerUserId = "d4000000-0000-0000-0000-000000000001";

        mockKeycloakForRoleAssignment();

        String assignJson = """
                { "user_id": "%s" }
                """.formatted(viewerUserId);

        mockMvc.perform(post("/api/v1/admin/roles/" + analystRoleId + "/users")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignJson))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/admin/roles/" + analystRoleId + "/users")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // --- Roles are immutable (cannot create or delete) ---

    @Test
    void rolesEndpoint_noPostForCreation() throws Exception {
        String json = """
                { "name": "PS_NEW_ROLE", "display_name": "New Role" }
                """;

        mockMvc.perform(post("/api/v1/admin/roles")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void rolesEndpoint_noDeleteForRemoval() throws Exception {
        String roleId = "10000000-0000-0000-0000-000000000001";

        mockMvc.perform(delete("/api/v1/admin/roles/" + roleId)
                        .with(adminJwt()))
                .andExpect(status().isMethodNotAllowed());
    }

    // --- Keycloak mock helper ---

    private void mockKeycloakForRoleAssignment() {
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

        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setName("PS_ANALYST");
        when(rolesResource.get(anyString())).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(roleRep);

        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
    }
}
