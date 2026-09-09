package com.vyoog.prospectsoul_backend.config;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
@EnableMethodSecurity
@Profile("!dev")
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource)
            throws Exception {
        return http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(requests -> requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                            HttpStatus.UNAUTHORIZED, "Full authentication is required to access this resource");
                    problem.setType(URI.create("about:blank"));
                    problem.setTitle("Unauthorized");
                    objectMapper.writeValue(response.getOutputStream(), problem);
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                            HttpStatus.FORBIDDEN, "You do not have permission to access this resource");
                    problem.setType(URI.create("about:blank"));
                    problem.setTitle("Forbidden");
                    objectMapper.writeValue(response.getOutputStream(), problem);
                }))
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
            Object rolesObj = realmAccess.get("roles");
            if (!(rolesObj instanceof Collection<?> roles)) {
                return List.of();
            }
            Stream<GrantedAuthority> realmRoles = roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(role -> role.startsWith("PS_"))
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role));

            Stream<GrantedAuthority> scopes = jwt.getClaimAsStringList("scope") != null
                    ? jwt.getClaimAsStringList("scope").stream()
                            .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                    : Stream.empty();

            return Stream.concat(realmRoles, scopes).toList();
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
