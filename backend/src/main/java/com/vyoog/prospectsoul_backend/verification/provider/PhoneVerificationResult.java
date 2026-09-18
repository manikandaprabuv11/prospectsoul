package com.vyoog.prospectsoul_backend.verification.provider;

import com.vyoog.prospectsoul_backend.verification.entity.VerificationFailureCode;

/**
 * Normalized provider outcome (docs/dev_docs/14 §6). The domain never sees a
 * Twilio payload, an HTTP status or an SDK type — only this record.
 *
 * @param providerName       provider that produced the result, e.g. {@code twilio}
 * @param providerReference  provider-side handle for the lookup, where the provider offers one
 * @param phoneNumber        E.164 number the provider echoed back
 * @param valid              provider's validity verdict; null when the call did not complete
 * @param lineType           provider's line classification, lower-cased; null when unavailable
 * @param failureCode        null when the lookup completed, otherwise the normalized reason
 * @param durationMs         wall-clock duration of the provider call, for observability
 */
public record PhoneVerificationResult(
        String providerName,
        String providerReference,
        String phoneNumber,
        Boolean valid,
        String lineType,
        String carrierName,
        String mobileCountryCode,
        String mobileNetworkCode,
        VerificationFailureCode failureCode,
        String failureMessage,
        long durationMs
) {

    /** True when the provider answered and the answer can be interpreted. */
    public boolean completed() {
        return failureCode == null;
    }

    public static PhoneVerificationResult failure(String providerName,
                                                  VerificationFailureCode code,
                                                  String message,
                                                  long durationMs) {
        return new PhoneVerificationResult(providerName, null, null, null, null, null, null, null,
                code, message, durationMs);
    }
}
