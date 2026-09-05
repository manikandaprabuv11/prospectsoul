package com.vyoog.prospectsoul_backend.config;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security infrastructure for the API.
 *
 * <p>
 * The service is a stateless OAuth2 resource server: bearer tokens are issued
 * by Keycloak and validated against the configured issuer. Only the health
 * probes are anonymous; everything else requires an authenticated principal.
 * Per-endpoint authorisation rules belong with the endpoints themselves and are
 * not defined here.
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource)
			throws Exception {
		return http
			.cors(cors -> cors.configurationSource(corsConfigurationSource))
			// No browser session and no cookie authentication, so there is no
			// CSRF surface to protect.
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(requests -> requests
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
				.anyRequest().authenticated())
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
			.build();
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
