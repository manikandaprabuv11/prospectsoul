package com.vyoog.prospectsoul_backend.verification.service;

import com.vyoog.prospectsoul_backend.verification.StubPhoneVerificationProvider;
import com.vyoog.prospectsoul_backend.verification.config.VerificationProperties;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationFailureCode;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationItemStatus;
import com.vyoog.prospectsoul_backend.verification.provider.LineTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The two pure decisions at the heart of the module: the success rule
 * ({@code decide}) and the retry budget ({@code applyRetryBudget}). No
 * database, no provider, no Spring context.
 */
class VerificationOutcomeTest {

    // Only the two pure methods are exercised, so every collaborator except
    // the properties (which carry the retry budget) is left null.
    private final VerificationItemProcessor processor = new VerificationItemProcessor(
            null, null, null, null, null, null, null,
            new VerificationProperties(null, null, null, 3, null, null, null, null),
            null);

    // --- success rule: valid == true AND line type == mobile ----------------

    @Test
    void validMobile_isVerified() {
        var outcome = processor.decide(StubPhoneVerificationProvider.mobile("+919876543210", "Airtel"));

        assertThat(outcome.status()).isEqualTo(VerificationItemStatus.VERIFIED);
        assertThat(outcome.code()).isNull();
    }

    @Test
    void invalidNumber_failsAsInvalid() {
        var outcome = processor.decide(StubPhoneVerificationProvider.invalid("+9112345"));

        assertThat(outcome.status()).isEqualTo(VerificationItemStatus.FAILED);
        assertThat(outcome.code()).isEqualTo(VerificationFailureCode.INVALID_NUMBER);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            LineTypes.LANDLINE, LineTypes.FIXED_VOIP, LineTypes.NON_FIXED_VOIP,
            LineTypes.TOLL_FREE, LineTypes.PREMIUM, LineTypes.SHARED_COST,
            LineTypes.UAN, LineTypes.VOICEMAIL, LineTypes.PAGER, LineTypes.UNKNOWN,
            LineTypes.PERSONAL,
    })
    void validButNonMobile_failsAsNonMobile(String lineType) {
        var outcome = processor.decide(
                StubPhoneVerificationProvider.lineType("+919876543210", lineType));

        assertThat(outcome.status()).isEqualTo(VerificationItemStatus.FAILED);
        assertThat(outcome.code()).isEqualTo(VerificationFailureCode.NON_MOBILE_LINE_TYPE);
        assertThat(outcome.message()).contains(lineType);
    }

    @Test
    void validWithNoLineType_failsAsNonMobile() {
        var outcome = processor.decide(
                StubPhoneVerificationProvider.lineType("+919876543210", null));

        assertThat(outcome.status()).isEqualTo(VerificationItemStatus.FAILED);
        assertThat(outcome.code()).isEqualTo(VerificationFailureCode.NON_MOBILE_LINE_TYPE);
    }

    @Test
    void providerFailure_carriesItsNormalizedCodeThrough() {
        var outcome = processor.decide(StubPhoneVerificationProvider.failure(
                VerificationFailureCode.PROVIDER_TIMEOUT, "read timed out"));

        assertThat(outcome.status()).isEqualTo(VerificationItemStatus.FAILED);
        assertThat(outcome.code()).isEqualTo(VerificationFailureCode.PROVIDER_TIMEOUT);
        assertThat(outcome.message()).isEqualTo("read timed out");
    }

    // --- retry budget --------------------------------------------------------

    @Test
    void retryableFailureWithBudgetLeft_goesBackToTheQueue() {
        var failed = new VerificationItemProcessor.Outcome(
                VerificationItemStatus.FAILED, VerificationFailureCode.RATE_LIMITED, "429");

        var effective = processor.applyRetryBudget(failed, 1);

        assertThat(effective.status()).isEqualTo(VerificationItemStatus.QUEUED);
        assertThat(effective.code()).isEqualTo(VerificationFailureCode.RATE_LIMITED);
    }

    @Test
    void retryableFailureOnTheLastAttempt_becomesMaxAttemptsExceeded() {
        var failed = new VerificationItemProcessor.Outcome(
                VerificationItemStatus.FAILED, VerificationFailureCode.PROVIDER_TIMEOUT, "timed out");

        var effective = processor.applyRetryBudget(failed, 3);

        assertThat(effective.status()).isEqualTo(VerificationItemStatus.FAILED);
        assertThat(effective.code()).isEqualTo(VerificationFailureCode.MAX_ATTEMPTS_EXCEEDED);
        assertThat(effective.message()).contains("giving up after 3 attempts");
    }

    @Test
    void permanentFailure_isNeverRetriedEvenOnTheFirstAttempt() {
        var failed = new VerificationItemProcessor.Outcome(
                VerificationItemStatus.FAILED, VerificationFailureCode.INVALID_NUMBER, "not valid");

        var effective = processor.applyRetryBudget(failed, 1);

        assertThat(effective.status()).isEqualTo(VerificationItemStatus.FAILED);
        assertThat(effective.code()).isEqualTo(VerificationFailureCode.INVALID_NUMBER);
    }

    @Test
    void successIsUntouchedByTheRetryBudget() {
        var success = new VerificationItemProcessor.Outcome(VerificationItemStatus.VERIFIED, null, null);

        assertThat(processor.applyRetryBudget(success, 3)).isEqualTo(success);
    }
}
