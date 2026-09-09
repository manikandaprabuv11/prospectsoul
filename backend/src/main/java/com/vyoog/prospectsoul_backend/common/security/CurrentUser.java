package com.vyoog.prospectsoul_backend.common.security;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class CurrentUser {

    private CurrentUser() {}

    public static String id(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getSubject();
        }
        throw new IllegalStateException("Expected JWT authentication");
    }

    public static String name(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getClaimAsString("preferred_username");
        }
        return "unknown";
    }

    public static List<String> roles(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_PS_"))
                .map(a -> a.substring(5))
                .toList();
    }
}
