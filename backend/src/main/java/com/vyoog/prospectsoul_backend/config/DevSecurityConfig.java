package com.vyoog.prospectsoul_backend.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@Profile("dev")
@Order(1)
@EnableConfigurationProperties(CorsProperties.class)
public class DevSecurityConfig {

    @Bean
    SecurityFilterChain devSecurityFilterChain(HttpSecurity http,
                                               CorsConfigurationSource corsConfigurationSource) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(requests -> requests
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

            @Bean
            JwtAuthenticationConverter jwtAuthenticationConverter() {
            JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
            converter.setJwtGrantedAuthoritiesConverter(jwt -> {
                Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                if (realmAccess == null) {
                return List.of();
                }

                Object rolesObject = realmAccess.get("roles");
                Stream<GrantedAuthority> realmRoles = rolesObject instanceof Collection<?> roles
                    ? roles.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .filter(role -> role.startsWith("PS_"))
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    : Stream.empty();

                List<String> scopes = jwt.getClaimAsStringList("scope");
                Stream<GrantedAuthority> scopeAuthorities = scopes == null
                    ? Stream.empty()
                    : scopes.stream().map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope));

                return Stream.concat(realmRoles, scopeAuthorities).toList();
            });
            return converter;
            }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
