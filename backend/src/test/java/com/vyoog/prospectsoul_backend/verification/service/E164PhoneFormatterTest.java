package com.vyoog.prospectsoul_backend.verification.service;

import com.vyoog.prospectsoul_backend.imports.normalization.PhoneNormalizer;
import com.vyoog.prospectsoul_backend.verification.config.VerificationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The bridge from the project's stored 10-digit national phone to the E.164
 * form Twilio Lookup requires. Normalization itself belongs to
 * {@link PhoneNormalizer} and is tested in {@code PhoneNormalizerTest}; these
 * tests cover only what this class adds.
 */
class E164PhoneFormatterTest {

    private final E164PhoneFormatter formatter = new E164PhoneFormatter(
            new PhoneNormalizer(),
            new VerificationProperties(null, null, null, null, null, null, null, "+91"));

    @ParameterizedTest
    @ValueSource(strings = {
            "9876543210",
            "+91 98765 43210",
            "0091-9876543210",
            "09876543210",
            "(987) 654-3210",
    })
    void everyStoredFormOfTheSameNumberProducesOneE164(String stored) {
        assertThat(formatter.toE164(stored)).isEqualTo("+919876543210");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "12345", "abcdefghij", "98765432101234"})
    void unusablePhoneYieldsNull(String stored) {
        assertThat(formatter.toE164(stored)).isNull();
    }

    @Test
    void nullPhoneYieldsNull() {
        assertThat(formatter.toE164(null)).isNull();
    }

    @Test
    void countryCodeIsConfigurable() {
        var ukFormatter = new E164PhoneFormatter(
                new PhoneNormalizer(),
                new VerificationProperties(null, null, null, null, null, null, null, "+44"));

        assertThat(ukFormatter.toE164("9876543210")).isEqualTo("+449876543210");
    }

    @Test
    void countryCodeFallsBackToIndiaWhenUnset() {
        var defaulted = new E164PhoneFormatter(
                new PhoneNormalizer(),
                new VerificationProperties(null, null, null, null, null, null, null, null));

        assertThat(defaulted.toE164("9876543210")).isEqualTo("+919876543210");
    }
}
