package com.vyoog.prospectsoul_backend.verification;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Overrides the provider with {@link StubPhoneVerificationProvider} for the
 * verification test slice, so success, invalid, non-mobile, retryable and
 * permanent branches can all be scripted deterministically.
 *
 * <p>The real {@code TwilioPhoneVerificationProvider} bean still exists in the
 * test context — every context needs a {@code PhoneVerificationProvider} — but
 * {@code src/test/resources/application.yaml} leaves its credentials empty, so
 * it short-circuits to {@code PROVIDER_NOT_CONFIGURED} before opening a
 * connection. Together with the {@code @Primary} override here, no test can
 * reach Twilio.
 */
@TestConfiguration(proxyBeanMethods = false)
public class VerificationTestConfiguration {

    @Bean
    @Primary
    StubPhoneVerificationProvider stubPhoneVerificationProvider() {
        return new StubPhoneVerificationProvider();
    }
}
