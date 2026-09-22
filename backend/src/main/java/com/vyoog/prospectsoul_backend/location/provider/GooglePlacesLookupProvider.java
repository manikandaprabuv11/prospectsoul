package com.vyoog.prospectsoul_backend.location.provider;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.vyoog.prospectsoul_backend.common.exception.BusinessRuleException;
import com.vyoog.prospectsoul_backend.location.dto.response.ExternalPlacesResponse;
import com.vyoog.prospectsoul_backend.location.entity.PincodeCentroid;
import com.vyoog.prospectsoul_backend.location.resolution.PincodeResolutionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Condition;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Real Google Places lookup — activates when {@code GOOGLE_PLACES_API_KEY}
 * (env / {@code prospectsoul.places.google-api-key}) is set. The key stays
 * strictly server-side; the frontend never sees it (UX Rule 9,
 * doc/dev_docs/21 §7.2).
 *
 * Uses the classic Nearby Search endpoint (v1 New Places API needs a
 * different signature and paid billing tier; the classic endpoint is
 * sufficient for the "find external businesses in a pincode" flow and is
 * the one docs 21 §7.1 references).
 */
@Component
@Primary
@Slf4j
@Conditional(GooglePlacesLookupProvider.KeyPresent.class)
public class GooglePlacesLookupProvider implements PlacesLookupProvider {

    private static final String NEARBY = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";

    private final PincodeResolutionService pincodeResolver;
    private final ObjectMapper objectMapper;

    @Value("${prospectsoul.places.google-api-key:}")
    private String apiKey;

    @Value("${prospectsoul.places.max-results:30}")
    private int maxResults;

    public static class KeyPresent implements Condition {
        @Override public boolean matches(ConditionContext c, AnnotatedTypeMetadata m) {
            String v = c.getEnvironment().getProperty("prospectsoul.places.google-api-key");
            return v != null && !v.isBlank();
        }
    }

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public GooglePlacesLookupProvider(PincodeResolutionService pincodeResolver,
                                       ObjectMapper objectMapper) {
        this.pincodeResolver = pincodeResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ExternalPlacesResponse.Result> search(String pincode, int radiusMeters,
                                                       String keyword, String type) {
        PincodeCentroid centroid;
        try {
            centroid = pincodeResolver.resolve(pincode);
        } catch (RuntimeException e) {
            log.debug("google_places: cannot resolve pincode {} ({}) — returning empty",
                    pincode, e.toString());
            return List.of();
        }
        BigDecimal lat = centroid.getLatitude();
        BigDecimal lng = centroid.getLongitude();

        StringBuilder url = new StringBuilder(NEARBY)
                .append("?location=").append(lat).append(',').append(lng)
                .append("&radius=").append(radiusMeters)
                .append("&key=").append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
        if (keyword != null && !keyword.isBlank())
            url.append("&keyword=").append(URLEncoder.encode(keyword, StandardCharsets.UTF_8));
        if (type != null && !type.isBlank())
            url.append("&type=").append(URLEncoder.encode(type, StandardCharsets.UTF_8));

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .timeout(Duration.ofSeconds(15))
                    .header("user-agent", "ProspectSoul/1.0")
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new BusinessRuleException(
                        "Google Places API returned " + resp.statusCode() + " — check your key");
            }
            return parseResponse(resp.body());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Google Places call failed", e);
            throw new BusinessRuleException("Google Places lookup failed: " + e.getMessage());
        }
    }

    private List<ExternalPlacesResponse.Result> parseResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String status = root.path("status").asText("");
            if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
                String err = root.path("error_message").asText(status);
                throw new BusinessRuleException("Google Places status " + status + ": " + err);
            }
            JsonNode results = root.path("results");
            if (!results.isArray()) return List.of();
            List<ExternalPlacesResponse.Result> out = new ArrayList<>();
            for (JsonNode r : results) {
                String id = r.path("place_id").asText(null);
                if (id == null) continue;
                String name = r.path("name").asText("");
                String address = r.path("vicinity").asText(r.path("formatted_address").asText(""));
                double lat = r.path("geometry").path("location").path("lat").asDouble();
                double lng = r.path("geometry").path("location").path("lng").asDouble();
                String bstatus = r.path("business_status").asText("OPERATIONAL");
                List<String> types = new ArrayList<>();
                JsonNode tarr = r.path("types");
                if (tarr.isArray()) for (JsonNode t : tarr) types.add(t.asText());
                out.add(new ExternalPlacesResponse.Result(
                        id, name, address, null,
                        BigDecimal.valueOf(lat), BigDecimal.valueOf(lng),
                        bstatus, types));
                if (out.size() >= maxResults) break;
            }
            return out;
        } catch (RuntimeException e) {
            throw e instanceof BusinessRuleException b ? b : new BusinessRuleException(
                    "Failed to parse Google Places response: " + e.getMessage());
        }
    }

    @Override
    public String providerName() { return "google_places"; }
}
