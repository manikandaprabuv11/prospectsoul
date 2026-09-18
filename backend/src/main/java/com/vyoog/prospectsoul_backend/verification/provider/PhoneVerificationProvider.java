package com.vyoog.prospectsoul_backend.verification.provider;

/**
 * The capability the verification domain talks to (docs/dev_docs/14 §4).
 *
 * <p>Business logic depends on this interface only, never on a vendor SDK, so
 * the provider can be replaced without touching the service or the worker —
 * the same discipline the project applies to its AI provider abstraction.
 */
public interface PhoneVerificationProvider {

    /**
     * Looks up one number. Implementations must not throw for provider-side
     * failures: they translate them into a
     * {@link PhoneVerificationResult#failure} carrying a normalized code.
     *
     * @param e164Phone number in E.164 form, e.g. {@code +919876543210}
     */
    PhoneVerificationResult verify(String e164Phone);

    /** Stable identifier persisted on every item, e.g. {@code twilio}. */
    String providerName();
}
