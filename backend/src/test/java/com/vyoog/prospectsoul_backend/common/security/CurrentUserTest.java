package com.vyoog.prospectsoul_backend.common.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserTest {

    private static Jwt buildJwt(String sub, String preferredUsername) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(sub)
                .claim("preferred_username", preferredUsername)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @Test
    void id_returnsSubjectFromJwt() {
        Jwt jwt = buildJwt("a1000000-0000-0000-0000-000000000001", "analyst");
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of());
        assertThat(CurrentUser.id(auth)).isEqualTo("a1000000-0000-0000-0000-000000000001");
    }

    @Test
    void id_throwsForNonJwtAuth() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("user", "pass");
        assertThatThrownBy(() -> CurrentUser.id(auth))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Expected JWT authentication");
    }

    @Test
    void name_returnsPreferredUsername() {
        Jwt jwt = buildJwt("sub-id", "analyst");
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of());
        assertThat(CurrentUser.name(auth)).isEqualTo("analyst");
    }

    @Test
    void name_returnsUnknownForNonJwtAuth() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("user", "pass");
        assertThat(CurrentUser.name(auth)).isEqualTo("unknown");
    }

    @Test
    void roles_extractsPsRolesAndStripsPrefix() {
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_PS_ANALYST"),
                new SimpleGrantedAuthority("ROLE_PS_ADMIN"),
                new SimpleGrantedAuthority("SCOPE_openid")
        );
        Jwt jwt = buildJwt("sub-id", "user");
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, authorities);
        assertThat(CurrentUser.roles(auth)).containsExactlyInAnyOrder("PS_ANALYST", "PS_ADMIN");
    }

    @Test
    void roles_returnsEmptyWhenNoPsRoles() {
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("SCOPE_openid")
        );
        Jwt jwt = buildJwt("sub-id", "user");
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, authorities);
        assertThat(CurrentUser.roles(auth)).isEmpty();
    }
}
