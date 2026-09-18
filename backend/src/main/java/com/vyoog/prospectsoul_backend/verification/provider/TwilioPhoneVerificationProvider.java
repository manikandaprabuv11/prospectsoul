package com.vyoog.prospectsoul_backend.verification.provider;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.vyoog.prospectsoul_backend.verification.config.TwilioProperties;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationFailureCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Twilio Lookup v2 Line Type Intelligence, called over its documented REST
 * contract:
 *
 * <pre>GET {base}/v2/PhoneNumbers/{e164}?Fields=line_type_intelligence</pre>
 *
 * <p>Deliberately no Twilio SDK dependency — the module needs exactly one
 * read-only endpoint, and {@code TWILIO_LOOKUP_BASE_URL} has to stay
 * redirectable at a stub in development and CI. The domain talks to
 * {@link PhoneVerificationProvider}, so swapping this out changes no business
 * logic.
 *
 * <p>Credentials are read from configuration and used only to build the Basic
 * auth header. Nothing in this class logs the account SID, the auth token or
 * the {@code Authorization} header (docs/dev_docs/14 §5, §17).
 */
@Component
@ConditionalOnProperty(name = "prospectsoul.verification.provider", havingValue = TwilioPhoneVerificationProvider.PROVIDER_NAME, matchIfMissing = true)
@Slf4j
public class TwilioPhoneVerificationProvider implements PhoneVerificationProvider {

    public static final String PROVIDER_NAME = "twilio";

    private final TwilioProperties properties;
    private final RestClient restClient;

    public TwilioPhoneVerificationProvider(TwilioProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.resolvedConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.resolvedReadTimeoutMs());

        this.restClient = RestClient.builder()
                .baseUrl(properties.resolvedBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public PhoneVerificationResult verify(String e164Phone) {
        long start = System.nanoTime();

        if (!properties.isConfigured()) {
            // A configuration failure is permanent: retrying cannot help, and
            // the operator needs to see it on the item rather than in a log.
            return PhoneVerificationResult.failure(PROVIDER_NAME,
                    VerificationFailureCode.PROVIDER_NOT_CONFIGURED,
                    "Twilio credentials are not configured. Set TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN.",
                    elapsedMs(start));
        }

        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/PhoneNumbers/{phone}")
                            .queryParam("Fields", "line_type_intelligence")
                            .build(e164Phone))
                    .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        if (status >= 400) {
                            return PhoneVerificationResult.failure(PROVIDER_NAME,
                                    classifyStatus(status),
                                    "Twilio lookup returned HTTP " + status,
                                    elapsedMs(start));
                        }
                        return interpret(response.bodyTo(TwilioLookupResponse.class),
                                e164Phone, elapsedMs(start));
                    }, false);
        } catch (ResourceAccessException e) {
            return PhoneVerificationResult.failure(PROVIDER_NAME,
                    VerificationFailureCode.PROVIDER_TIMEOUT,
                    "Twilio lookup did not respond: " + describe(e),
                    elapsedMs(start));
        } catch (RuntimeException e) {
            return PhoneVerificationResult.failure(PROVIDER_NAME,
                    VerificationFailureCode.PROVIDER_ERROR,
                    "Twilio lookup failed: " + e.getMessage(),
                    elapsedMs(start));
        }
    }

    /**
     * Turns one Lookup response into a normalized result. Kept separate from
     * the HTTP call so the mapping is unit-testable without a network.
     */
    PhoneVerificationResult interpret(TwilioLookupResponse body, String requestedPhone, long durationMs) {
        if (body == null) {
            return PhoneVerificationResult.failure(PROVIDER_NAME,
                    VerificationFailureCode.PROVIDER_ERROR,
                    "Twilio lookup returned an empty body", durationMs);
        }

        TwilioLookupResponse.LineTypeIntelligence intelligence = body.lineTypeIntelligence();
        String lineType = intelligence == null ? null : LineTypes.normalize(intelligence.type());

        return new PhoneVerificationResult(
                PROVIDER_NAME,
                body.url(),
                body.phoneNumber() != null ? body.phoneNumber() : requestedPhone,
                body.valid(),
                lineType,
                intelligence == null ? null : intelligence.carrierName(),
                intelligence == null ? null : intelligence.mobileCountryCode(),
                intelligence == null ? null : intelligence.mobileNetworkCode(),
                null,
                null,
                durationMs);
    }

    /**
     * Maps a provider HTTP status to the normalized retry classification of
     * docs/dev_docs/14 §10.
     */
    static VerificationFailureCode classifyStatus(int status) {
        if (status == 404) {
            // Twilio answers 404 for a number it cannot parse at all.
            return VerificationFailureCode.INVALID_NUMBER;
        }
        if (status == 400) {
            return VerificationFailureCode.MALFORMED_REQUEST;
        }
        if (status == 401 || status == 403) {
            return VerificationFailureCode.PROVIDER_AUTH_ERROR;
        }
        if (status == 429) {
            return VerificationFailureCode.RATE_LIMITED;
        }
        if (status >= 500) {
            return VerificationFailureCode.PROVIDER_UNAVAILABLE;
        }
        return VerificationFailureCode.PROVIDER_ERROR;
    }

    private String basicAuthHeader() {
        String credentials = properties.accountSid() + ":" + properties.authToken();
        return "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private static String describe(ResourceAccessException e) {
        Throwable cause = e.getCause();
        return cause != null && cause.getMessage() != null ? cause.getMessage() : e.getMessage();
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
