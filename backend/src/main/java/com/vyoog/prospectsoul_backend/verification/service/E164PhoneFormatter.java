package com.vyoog.prospectsoul_backend.verification.service;

import com.vyoog.prospectsoul_backend.imports.normalization.PhoneNormalizer;
import com.vyoog.prospectsoul_backend.verification.config.VerificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Bridges the project's existing phone normalization to the E.164 form Twilio
 * Lookup requires.
 *
 * <p>{@link PhoneNormalizer} is the project's single source of truth for phone
 * normalization (it strips {@code +91}/{@code 0091}/leading {@code 0} and
 * punctuation down to 10 national digits). This class does not re-implement
 * any of that; it only prefixes the configured dialling code, because a
 * 10-digit national number is not a valid Lookup input.
 */
@Component
@RequiredArgsConstructor
public class E164PhoneFormatter {

    private final PhoneNormalizer phoneNormalizer;
    private final VerificationProperties properties;

    /**
     * @return E.164 form, or {@code null} when the stored phone is missing or
     *         is not a usable 10-digit national number
     */
    public String toE164(String storedPhone) {
        String normalized = phoneNormalizer.normalize(storedPhone);
        if (!phoneNormalizer.isValid(normalized)) {
            return null;
        }
        return properties.resolvedCountryCode() + normalized;
    }
}
