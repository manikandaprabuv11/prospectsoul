package com.vyoog.prospectsoul_backend.verification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Twilio credentials, supplied by environment only (docs/dev_docs/14 §5):
 * {@code TWILIO_ACCOUNT_SID}, {@code TWILIO_AUTH_TOKEN},
 * {@code TWILIO_LOOKUP_BASE_URL}.
 *
 * <p>These values are never logged, never returned in a DTO and never reach
 * the frontend. Any credential that has been pasted into a conversation, a
 * sample or a prior session must be treated as compromised and rotated before
 * production use.
 *
 * @param connectTimeoutMs connect timeout for the lookup call
 * @param readTimeoutMs    read timeout for the lookup call
 */
@ConfigurationProperties(prefix = "prospectsoul.verification.twilio")
public record TwilioProperties(
        String accountSid,
        String authToken,
        String lookupBaseUrl,
        Integer connectTimeoutMs,
        Integer readTimeoutMs
) {

    private static final String UNCONFIGURED = "not-configured";

    /** True only when both credentials are present and not the placeholder. */
    public boolean isConfigured() {
        return isReal(accountSid) && isReal(authToken);
    }

    public String resolvedBaseUrl() {
        String url = lookupBaseUrl == null || lookupBaseUrl.isBlank()
                ? "https://lookups.twilio.com"
                : lookupBaseUrl.trim();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public int resolvedConnectTimeoutMs() {
        return connectTimeoutMs == null || connectTimeoutMs < 100 ? 5_000 : connectTimeoutMs;
    }

    public int resolvedReadTimeoutMs() {
        return readTimeoutMs == null || readTimeoutMs < 100 ? 10_000 : readTimeoutMs;
    }

    private static boolean isReal(String value) {
        return value != null && !value.isBlank() && !UNCONFIGURED.equalsIgnoreCase(value.trim());
    }
}
