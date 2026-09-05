package com.vyoog.prospectsoul_backend.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cross-origin settings for the API. Origins are supplied per environment so
 * that a wildcard is never needed; the development default is the Vite dev
 * server.
 *
 * @param allowedOrigins exact origins permitted to call the API
 */
@ConfigurationProperties(prefix = "prospectsoul.cors")
public record CorsProperties(List<String> allowedOrigins) {

	public CorsProperties {
		allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
	}

}
