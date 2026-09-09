package com.vyoog.prospectsoul_backend.config;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class JwtRoleExtractionTest {

    private JwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        SecurityConfig config = new SecurityConfig(new ObjectMapper());
        converter = config.jwtAuthenticationConverter();
    }

    private Jwt buildJwt(List<String> realmRoles) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-id")
                .claim("realm_access", Map.of("roles", realmRoles))
                .claim("scope", List.of("openid", "profile", "email"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @Test
    void extractsPsRolesWithRolePrefix() {
        Jwt jwt = buildJwt(List.of("PS_ANALYST", "PS_ADMIN", "default-roles-vyoog"));
        Collection<GrantedAuthority> authorities = converter.convert(jwt).getAuthorities();

        List<String> authorityStrings = authorities.stream()
                .map(GrantedAuthority::getAuthority).toList();

        assertThat(authorityStrings).contains("ROLE_PS_ANALYST", "ROLE_PS_ADMIN");
        assertThat(authorityStrings).doesNotContain("ROLE_default-roles-vyoog");
    }

    @Test
    void filterOutNonPsRoles() {
        Jwt jwt = buildJwt(List.of("uma_authorization", "offline_access", "PS_VIEWER"));
        Collection<GrantedAuthority> authorities = converter.convert(jwt).getAuthorities();

        List<String> authorityStrings = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .toList();

        assertThat(authorityStrings).containsExactly("ROLE_PS_VIEWER");
    }

    @Test
    void includesScopes() {
        Jwt jwt = buildJwt(List.of("PS_COO"));
        Collection<GrantedAuthority> authorities = converter.convert(jwt).getAuthorities();

        List<String> authorityStrings = authorities.stream()
                .map(GrantedAuthority::getAuthority).toList();

        assertThat(authorityStrings).contains("ROLE_PS_COO", "SCOPE_openid", "SCOPE_profile", "SCOPE_email");
    }

    @Test
    void handlesNoRealmAccess() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-id")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt).getAuthorities();

        List<String> authorityStrings = authorities.stream()
                .map(GrantedAuthority::getAuthority).toList();

        assertThat(authorityStrings.stream().filter(a -> a.startsWith("ROLE_"))).isEmpty();
    }

    @Test
    void handlesEmptyRoles() {
        Jwt jwt = buildJwt(List.of());
        Collection<GrantedAuthority> authorities = converter.convert(jwt).getAuthorities();

        List<String> roleAuthorities = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .toList();

        assertThat(roleAuthorities).isEmpty();
    }

    @Test
    void extractsAllFivePsRoles() {
        Jwt jwt = buildJwt(List.of("PS_ANALYST", "PS_SALES_LEAD", "PS_ADMIN", "PS_VIEWER", "PS_COO"));
        Collection<GrantedAuthority> authorities = converter.convert(jwt).getAuthorities();

        List<String> roleAuthorities = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_PS_"))
                .toList();

        assertThat(roleAuthorities).containsExactlyInAnyOrder(
                "ROLE_PS_ANALYST", "ROLE_PS_SALES_LEAD", "ROLE_PS_ADMIN", "ROLE_PS_VIEWER", "ROLE_PS_COO");
    }
}
