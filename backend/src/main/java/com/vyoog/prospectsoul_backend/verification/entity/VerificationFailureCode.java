package com.vyoog.prospectsoul_backend.verification.entity;

/**
 * The single normalized failure/skip vocabulary for the module. Provider HTTP
 * and transport errors are translated into these codes by the provider
 * implementation (docs/dev_docs/14 §6, §10) so that no Twilio-specific status
 * leaks into the domain.
 *
 * <p>{@link #retryable()} is the retry classification from §10: only timeouts,
 * rate limits and temporary 5xx are retried. An invalid number, a non-mobile
 * line type, a malformed request and an authentication/configuration failure
 * are permanent and are never retried.
 */
public enum VerificationFailureCode {

    // --- Eligibility / skip reasons (never reach the provider) ---------------
    /** Company has no usable primary phone (docs/dev_docs/13 §6). */
    NO_PHONE(false),
    /** Company was already VERIFIED when the batch started (docs/dev_docs/13 §6). */
    ALREADY_VERIFIED(false),
    /** Company is archived, disqualified-as-deleted, or otherwise not workable. */
    NOT_ELIGIBLE(false),
    /** Company does not match the Added By / date filters the batch was created with. */
    FILTER_MISMATCH(false),

    // --- Permanent provider outcomes ----------------------------------------
    /** Provider reports the number is not a valid phone number. */
    INVALID_NUMBER(false),
    /** Provider reports a valid number whose line type is not mobile. */
    NON_MOBILE_LINE_TYPE(false),
    /** The request we sent was rejected as malformed (HTTP 400). */
    MALFORMED_REQUEST(false),
    /** Credentials rejected (HTTP 401/403). */
    PROVIDER_AUTH_ERROR(false),
    /** No provider credentials configured in this environment. */
    PROVIDER_NOT_CONFIGURED(false),
    /** Provider responded, but the response could not be understood. */
    PROVIDER_ERROR(false),

    // --- Transient provider outcomes ----------------------------------------
    /** Connect/read timeout. */
    PROVIDER_TIMEOUT(true),
    /** HTTP 429. */
    RATE_LIMITED(true),
    /** Temporary HTTP 5xx. */
    PROVIDER_UNAVAILABLE(true),

    // --- Worker outcome ------------------------------------------------------
    /** A retryable failure exhausted the configured attempt budget. */
    MAX_ATTEMPTS_EXCEEDED(false);

    private final boolean retryable;

    VerificationFailureCode(boolean retryable) {
        this.retryable = retryable;
    }

    /** True when the worker should re-queue the item rather than fail it. */
    public boolean retryable() {
        return retryable;
    }
}
