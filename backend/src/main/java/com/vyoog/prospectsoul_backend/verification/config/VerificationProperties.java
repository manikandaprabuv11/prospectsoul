package com.vyoog.prospectsoul_backend.verification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Verification module configuration (docs/dev_docs/14 §16).
 *
 * <p>Bound under {@code prospectsoul.verification}; every value is overridable
 * by an environment variable, following the same pattern as the existing
 * {@code prospectsoul.*} settings.
 *
 * @param workerEnabled     master switch for the background worker
 * @param pollIntervalMs    how often the worker looks for queued work
 * @param batchSize         how many items one worker pass claims
 * @param maxRetries        maximum attempts per item before it is failed
 * @param provider          which provider bean to use, e.g. {@code twilio}
 * @param maxBatchCompanies cap on companies per batch, rejected with 422 above it
 * @param staleItemTimeoutMs how long an item may sit in PROCESSING before it is
 *                           treated as abandoned by a dead JVM and re-queued
 * @param defaultCountryCode dialling code prefixed to a 10-digit national number
 *                           to build the E.164 form the provider requires
 */
@ConfigurationProperties(prefix = "prospectsoul.verification")
public record VerificationProperties(
        Boolean workerEnabled,
        Long pollIntervalMs,
        Integer batchSize,
        Integer maxRetries,
        String provider,
        Integer maxBatchCompanies,
        Long staleItemTimeoutMs,
        String defaultCountryCode
) {

    public boolean isWorkerEnabled() {
        return workerEnabled == null || workerEnabled;
    }

    public int resolvedBatchSize() {
        return batchSize == null || batchSize < 1 ? 10 : batchSize;
    }

    public int resolvedMaxRetries() {
        return maxRetries == null || maxRetries < 1 ? 3 : maxRetries;
    }

    public int resolvedMaxBatchCompanies() {
        return maxBatchCompanies == null || maxBatchCompanies < 1 ? 1000 : maxBatchCompanies;
    }

    public long resolvedStaleItemTimeoutMs() {
        return staleItemTimeoutMs == null || staleItemTimeoutMs < 1000 ? 300_000 : staleItemTimeoutMs;
    }

    public String resolvedCountryCode() {
        return defaultCountryCode == null || defaultCountryCode.isBlank() ? "+91" : defaultCountryCode.trim();
    }
}
