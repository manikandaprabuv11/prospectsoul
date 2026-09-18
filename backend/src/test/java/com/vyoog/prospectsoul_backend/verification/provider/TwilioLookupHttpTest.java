package com.vyoog.prospectsoul_backend.verification.provider;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.vyoog.prospectsoul_backend.verification.config.TwilioProperties;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationFailureCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The real provider against a local HTTP stub.
 *
 * <p>{@link TwilioPhoneVerificationProviderTest} covers the response mapping as
 * a pure function; this covers everything around it that only executes over a
 * socket — the request URI and query parameter, the Basic auth header,
 * deserialising Twilio's snake_case body through the application's JSON
 * configuration, and the HTTP status classification. Without this, the
 * {@code RestClient} wiring would ship untested.
 *
 * <p>The stub is an in-process {@code HttpServer} on an ephemeral port, so no
 * new dependency and no network access are involved. Nothing here talks to
 * Twilio.
 */
class TwilioLookupHttpTest {

    private HttpServer server;
    private String baseUrl;

    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private final AtomicReference<String> lastQuery = new AtomicReference<>();
    private final AtomicReference<String> lastAuthorization = new AtomicReference<>();
    private final AtomicInteger responseStatus = new AtomicInteger(200);
    private final AtomicReference<String> responseBody = new AtomicReference<>("{}");
    private final List<String> requests = new CopyOnWriteArrayList<>();

    @BeforeEach
    void startStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopStub() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        lastPath.set(exchange.getRequestURI().getPath());
        lastQuery.set(exchange.getRequestURI().getQuery());
        lastAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        requests.add(exchange.getRequestURI().toString());

        byte[] body = responseBody.get().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(responseStatus.get(), body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private TwilioPhoneVerificationProvider provider() {
        return new TwilioPhoneVerificationProvider(
                new TwilioProperties("ACtestsid", "testtoken", baseUrl, 2000, 2000));
    }

    @Test
    void callsTheDocumentedLookupV2EndpointWithLineTypeIntelligence() {
        responseBody.set("""
                {
                  "calling_country_code": "91",
                  "country_code": "IN",
                  "phone_number": "+919876543210",
                  "valid": true,
                  "validation_errors": [],
                  "line_type_intelligence": {
                    "carrier_name": "Bharti Airtel",
                    "mobile_country_code": "404",
                    "mobile_network_code": "90",
                    "type": "mobile",
                    "error_code": null
                  },
                  "url": "https://lookups.twilio.com/v2/PhoneNumbers/+919876543210"
                }
                """);

        PhoneVerificationResult result = provider().verify("+919876543210");

        assertThat(lastPath.get()).isEqualTo("/v2/PhoneNumbers/+919876543210");
        assertThat(lastQuery.get()).contains("Fields=line_type_intelligence");

        assertThat(result.completed()).isTrue();
        assertThat(result.valid()).isTrue();
        assertThat(result.lineType()).isEqualTo(LineTypes.MOBILE);
        assertThat(result.carrierName()).isEqualTo("Bharti Airtel");
        assertThat(result.mobileCountryCode()).isEqualTo("404");
        assertThat(result.mobileNetworkCode()).isEqualTo("90");
        assertThat(result.phoneNumber()).isEqualTo("+919876543210");
        assertThat(result.providerReference())
                .isEqualTo("https://lookups.twilio.com/v2/PhoneNumbers/+919876543210");
        assertThat(result.providerName()).isEqualTo("twilio");
    }

    @Test
    void theE164PlusIsPercentEncodedInTheRequestLineAndDecodesBackToTheNumber() {
        // Worth pinning: the request line carries `%2B919876543210`, not a
        // literal `+`. That is correct RFC-3986 encoding of a path segment,
        // and any conformant server — Twilio included — decodes it back to
        // `+919876543210`, which is what `getPath()` below reads. A stub that
        // matches on the raw request line instead of the decoded path will
        // wrongly 404 every lookup.
        responseBody.set("{\"valid\": true, \"line_type_intelligence\": {\"type\": \"mobile\"}}");

        provider().verify("+919876543210");

        assertThat(requests).hasSize(1);
        assertThat(requests.getFirst()).contains("%2B919876543210");
        assertThat(lastPath.get()).isEqualTo("/v2/PhoneNumbers/+919876543210");
    }

    @Test
    void sendsBasicAuthBuiltFromTheConfiguredCredentials() {
        responseBody.set("{\"valid\": true, \"line_type_intelligence\": {\"type\": \"mobile\"}}");

        provider().verify("+919876543210");

        String header = lastAuthorization.get();
        assertThat(header).startsWith("Basic ");
        String decoded = new String(
                Base64.getDecoder().decode(header.substring("Basic ".length())),
                StandardCharsets.UTF_8);
        assertThat(decoded).isEqualTo("ACtestsid:testtoken");
    }

    @Test
    void parsesCamelCaseLineTypesIntoTheNormalizedForm() {
        responseBody.set("""
                {
                  "phone_number": "+919876543210",
                  "valid": true,
                  "line_type_intelligence": {"type": "nonFixedVoip", "carrier_name": "Some VoIP"}
                }
                """);

        PhoneVerificationResult result = provider().verify("+919876543210");

        assertThat(result.lineType()).isEqualTo(LineTypes.NON_FIXED_VOIP);
    }

    @Test
    void ignoresUnknownFieldsSoAProviderAdditionCannotBreakABatch() {
        responseBody.set("""
                {
                  "phone_number": "+919876543210",
                  "valid": true,
                  "line_type_intelligence": {"type": "mobile", "brand_new_field": "surprise"},
                  "some_new_top_level_block": {"anything": [1, 2, 3]}
                }
                """);

        PhoneVerificationResult result = provider().verify("+919876543210");

        assertThat(result.completed()).isTrue();
        assertThat(result.lineType()).isEqualTo(LineTypes.MOBILE);
    }

    @Test
    void invalidNumberComesBackAsCompletedButNotValid() {
        responseBody.set("""
                {
                  "phone_number": "+9112345",
                  "valid": false,
                  "validation_errors": ["TOO_SHORT"],
                  "line_type_intelligence": null
                }
                """);

        PhoneVerificationResult result = provider().verify("+9112345");

        assertThat(result.completed()).isTrue();
        assertThat(result.valid()).isFalse();
        assertThat(result.lineType()).isNull();
    }

    @Test
    void http404IsAPermanentInvalidNumber() {
        responseStatus.set(404);
        responseBody.set("{\"code\": 20404, \"message\": \"not found\"}");

        PhoneVerificationResult result = provider().verify("+910000000000");

        assertThat(result.completed()).isFalse();
        assertThat(result.failureCode()).isEqualTo(VerificationFailureCode.INVALID_NUMBER);
        assertThat(result.failureCode().retryable()).isFalse();
    }

    @Test
    void http401IsAPermanentAuthFailureAndLeaksNoCredential() {
        responseStatus.set(401);
        responseBody.set("{\"message\": \"Authenticate\"}");

        PhoneVerificationResult result = provider().verify("+919876543210");

        assertThat(result.failureCode()).isEqualTo(VerificationFailureCode.PROVIDER_AUTH_ERROR);
        assertThat(result.failureCode().retryable()).isFalse();
        assertThat(result.failureMessage())
                .doesNotContain("ACtestsid", "testtoken", "Basic ");
    }

    @Test
    void http429IsRetryable() {
        responseStatus.set(429);

        PhoneVerificationResult result = provider().verify("+919876543210");

        assertThat(result.failureCode()).isEqualTo(VerificationFailureCode.RATE_LIMITED);
        assertThat(result.failureCode().retryable()).isTrue();
    }

    @Test
    void http503IsRetryable() {
        responseStatus.set(503);

        PhoneVerificationResult result = provider().verify("+919876543210");

        assertThat(result.failureCode()).isEqualTo(VerificationFailureCode.PROVIDER_UNAVAILABLE);
        assertThat(result.failureCode().retryable()).isTrue();
    }

    @Test
    void http400IsAPermanentMalformedRequest() {
        responseStatus.set(400);

        PhoneVerificationResult result = provider().verify("+919876543210");

        assertThat(result.failureCode()).isEqualTo(VerificationFailureCode.MALFORMED_REQUEST);
        assertThat(result.failureCode().retryable()).isFalse();
    }

    @Test
    void anUnreachableProviderIsARetryableTimeout() {
        // Port 1 on loopback refuses immediately, which is the connect-failure
        // branch a real outage takes.
        var unreachable = new TwilioPhoneVerificationProvider(
                new TwilioProperties("ACtestsid", "testtoken", "http://127.0.0.1:1", 300, 300));

        PhoneVerificationResult result = unreachable.verify("+919876543210");

        assertThat(result.completed()).isFalse();
        assertThat(result.failureCode()).isEqualTo(VerificationFailureCode.PROVIDER_TIMEOUT);
        assertThat(result.failureCode().retryable()).isTrue();
    }

    @Test
    void makesExactlyOneRequestPerVerifyCall() {
        responseBody.set("{\"valid\": true, \"line_type_intelligence\": {\"type\": \"mobile\"}}");

        provider().verify("+919876543210");

        assertThat(requests).hasSize(1);
    }

    @Test
    void withoutCredentialsNoRequestIsMadeAtAll() {
        var unconfigured = new TwilioPhoneVerificationProvider(
                new TwilioProperties("", "", baseUrl, 2000, 2000));

        PhoneVerificationResult result = unconfigured.verify("+919876543210");

        assertThat(result.failureCode()).isEqualTo(VerificationFailureCode.PROVIDER_NOT_CONFIGURED);
        assertThat(requests).isEmpty();
    }
}
