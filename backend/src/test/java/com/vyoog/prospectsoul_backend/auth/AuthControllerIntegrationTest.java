package com.vyoog.prospectsoul_backend.auth;

import java.util.List;

import com.vyoog.prospectsoul_backend.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Keycloak keycloak;

    @Test
    void forgotPassword_validEmail_returns200() throws Exception {
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);
        when(keycloak.realm("vyoog")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        UserRepresentation user = new UserRepresentation();
        user.setId("a1000000-0000-0000-0000-000000000001");
        user.setEmail("priya@vyoog.com");
        when(usersResource.searchByEmail("priya@vyoog.com", true)).thenReturn(List.of(user));
        when(usersResource.get("a1000000-0000-0000-0000-000000000001")).thenReturn(userResource);

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"priya@vyoog.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(userResource).executeActionsEmail(List.of("UPDATE_PASSWORD"));
    }

    @Test
    void forgotPassword_unknownEmail_returns200NoEnumeration() throws Exception {
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        when(keycloak.realm("vyoog")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.searchByEmail("nobody@example.com", true)).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"nobody@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(usersResource, never()).get(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void forgotPassword_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forgotPassword_missingEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forgotPassword_noJwtRequired() throws Exception {
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        when(keycloak.realm("vyoog")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.searchByEmail("test@example.com", true)).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"test@example.com\"}"))
                .andExpect(status().isOk());
    }
}
