package com.vyoog.prospectsoul_backend.verification.provider;

import com.vyoog.prospectsoul_backend.verification.StubPhoneVerificationProvider;

import com.vyoog.prospectsoul_backend.verification.config.TwilioProperties;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationFailureCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Provider response mapping and error classification (docs/dev_docs/14 §6,
 * §7, §10). Pure unit tests — no HTTP, no credentials.
 */
class TwilioPhoneVerificationProviderTest {

    private static final TwilioProperties CONFIGURED = new TwilioProperties(
            "ACtest", "token", "https://lookups.example", 1000, 1000);
    private static final TwilioProperties UNCONFIGURED = new TwilioProperties(
            null, null, null, null, null);

    private final TwilioPhoneVerificationProvider provider =
            new TwilioPhoneVerificationProvider(CONFIGURED);

    @Test
    void providerName_isTwilio() {
        assertThat(provider.providerName()).isEqualTo("twilio");
    }

    @Test
    void interpret_validMobile_mapsEveryField() {
        var body = new TwilioLookupResponse(
                "+919876543210", "IN", "91", true, java.util.List.of(),
                new TwilioLookupResponse.LineTypeIntelligence(
                        "mobile", "Airtel", "404", "90", null),
                "https://lookups.twilio.com/v2/PhoneNumbers/+919876543210");

        PhoneVerificationResult result = provider.interpret(body, "+919876543210", 42);

        assertThat(result.completed()).isTrue();
        assertThat(result.valid()).isTrue();
        assertThat(result.lineType()).isEqualTo(LineTypes.MOBILE);
        assertThat(result.carrierName()).isEqualTo("Airtel");
        assertThat(result.mobileCountryCode()).isEqualTo("404");
        assertThat(result.mobileNetworkCode()).isEqualTo("90");
        assertThat(result.phoneNumber()).isEqualTo("+919876543210");
        assertThat(result.providerReference())
                .isEqualTo("https://lookups.twilio.com/v2/PhoneNumbers/+919876543210");
        assertThat(result.durationMs()).isEqualTo(42);
        assertThat(result.failureCode()).isNull();
    }

    @Test
    void interpret_camelCaseLineType_isNormalizedNotRejected() {
        var body = new TwilioLookupResponse(
                "+919876543210", "IN", "91", true, java.util.List.of(),
                new TwilioLookupResponse.LineTypeIntelligence(
                        "nonFixedVoip", "Some VoIP", null, null, null),
                null);

        PhoneVerificationResult result = provider.interpret(body, "+919876543210", 1);

        assertThat(result.lineType()).isEqualTo(LineTypes.NON_FIXED_VOIP);
    }

    @Test
    void interpret_invalidNumber_keepsValidFalse() {
        var body = new TwilioLookupResponse(
                "+9112345", "IN", "91", false, java.util.List.of("TOO_SHORT"), null, null);

        PhoneVerificationResult result = provider.interpret(body, "+9112345", 1);

        assertThat(result.completed()).isTrue();
        assertThat(result.valid()).isFalse();
        assertThat(result.lineType()).isNull();
    }

    @Test
    void interpret_missingLineTypeBlock_doesNotThrow() {
        var body = new TwilioLookupResponse(
                "+919876543210", "IN", "91", true, java.util.List.of(), null, null);

        PhoneVerificationResult result = provider.interpret(body, "+919876543210", 1);

        assertThat(result.completed()).isTrue();
        assertThat(result.lineType()).isNull();
        assertThat(result.carrierName()).isNull();
    }

    @Test
    void interpret_emptyBody_isAProviderError() {
        PhoneVerificationResult result = provider.interpret(null, "+919876543210", 1);

        assertThat(result.completed()).isFalse();
        assertThat(result.failureCode()).isEqualTo(VerificationFailureCode.PROVIDER_ERROR);
    }

    @Test
    void verify_withoutCredentials_failsPermanentlyAndNeverCallsOut() {
        var unconfigured = new TwilioPhoneVerificationProvider(UNCONFIGURED);

        PhoneVerificationResult result = unconfigured.verify("+919876543210");

        assertThat(result.completed()).isFalse();
        assertThat(result.failureCode()).isEqualTo(VerificationFailureCode.PROVIDER_NOT_CONFIGURED);
        assertThat(result.failureCode().retryable()).isFalse();
        assertThat(result.failureMessage()).contains("TWILIO_ACCOUNT_SID");
    }

    @Test
    void failureMessage_neverContainsCredentials() {
        var unconfigured = new TwilioPhoneVerificationProvider(
                new TwilioProperties("ACsecretsid", "supersecrettoken", null, null, null));

        PhoneVerificationResult result = unconfigured.interpret(null, "+919876543210", 1);

        assertThat(result.failureMessage()).doesNotContain("ACsecretsid", "supersecrettoken");
    }

    @ParameterizedTest
    @CsvSource({
            "400, MALFORMED_REQUEST,     false",
            "401, PROVIDER_AUTH_ERROR,   false",
            "403, PROVIDER_AUTH_ERROR,   false",
            "404, INVALID_NUMBER,        false",
            "429, RATE_LIMITED,          true",
            "500, PROVIDER_UNAVAILABLE,  true",
            "503, PROVIDER_UNAVAILABLE,  true",
            "418, PROVIDER_ERROR,        false",
    })
    void classifyStatus_matchesTheRetryContract(int status, VerificationFailureCode expected, boolean retryable) {
        VerificationFailureCode code = TwilioPhoneVerificationProvider.classifyStatus(status);

        assertThat(code).isEqualTo(expected);
        assertThat(code.retryable()).isEqualTo(retryable);
    }

    @Test
    void timeout_isClassifiedRetryable() {
        assertThat(VerificationFailureCode.PROVIDER_TIMEOUT.retryable()).isTrue();
    }

    @Test
    void permanentCodes_areNeverRetried() {
        assertThat(VerificationFailureCode.INVALID_NUMBER.retryable()).isFalse();
        assertThat(VerificationFailureCode.NON_MOBILE_LINE_TYPE.retryable()).isFalse();
        assertThat(VerificationFailureCode.MALFORMED_REQUEST.retryable()).isFalse();
        assertThat(VerificationFailureCode.PROVIDER_AUTH_ERROR.retryable()).isFalse();
        assertThat(VerificationFailureCode.PROVIDER_NOT_CONFIGURED.retryable()).isFalse();
        assertThat(VerificationFailureCode.MAX_ATTEMPTS_EXCEEDED.retryable()).isFalse();
    }

    @Test
    void baseUrl_trailingSlashIsTrimmed() {
        var properties = new TwilioProperties("sid", "token", "https://lookups.example/", null, null);

        assertThat(properties.resolvedBaseUrl()).isEqualTo("https://lookups.example");
    }

    @Test
    void placeholderCredentials_countAsUnconfigured() {
        assertThat(new TwilioProperties("not-configured", "not-configured", null, null, null)
                .isConfigured()).isFalse();
        assertThat(new TwilioProperties("", "  ", null, null, null).isConfigured()).isFalse();
        assertThat(new TwilioProperties("ACreal", "realtoken", null, null, null)
                .isConfigured()).isTrue();
    }

    @Test
    void lineTypes_mobileDetectionIsCaseInsensitive() {
        assertThat(LineTypes.isMobile("mobile")).isTrue();
        assertThat(LineTypes.isMobile("Mobile")).isTrue();
        assertThat(LineTypes.isMobile(" MOBILE ")).isTrue();
        assertThat(LineTypes.isMobile("landline")).isFalse();
        assertThat(LineTypes.isMobile(null)).isFalse();
    }
}
