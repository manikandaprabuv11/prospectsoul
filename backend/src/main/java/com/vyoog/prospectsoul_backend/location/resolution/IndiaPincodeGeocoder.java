package com.vyoog.prospectsoul_backend.location.resolution;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Live pincode geocoder for India. Combines two free public APIs:
 *
 *   1. {@code api.postalpincode.in} — India Post's official metadata
 *      (area, district, state, Delivery status) but no coordinates.
 *   2. Nominatim (OpenStreetMap) — real lat/lng for
 *      {@code "<area>, <district>, India"} or, when postalpincode.in has
 *      no data, just {@code "<pincode>, India"}.
 *
 * Neither service needs an API key. Both are subject to fair-use policies
 * — the resolver caches every result into {@code pincode_centroids} so a
 * given pincode is fetched at most once. A custom User-Agent is set on
 * every call per Nominatim's usage policy.
 *
 * If the pincode is genuinely unknown (postalpincode.in returns
 * {@code Error} AND Nominatim finds nothing) the method returns
 * {@link Optional#empty()}; the caller surfaces that as 404 to the user.
 */
@Component
@Slf4j
@ConditionalOnProperty(
        prefix = "prospectsoul.pincode",
        name = "geocoder",
        havingValue = "india_post_nominatim",
        matchIfMissing = true)
public class IndiaPincodeGeocoder implements PincodeGeocoder {

    private final ObjectMapper objectMapper;

    @Value("${prospectsoul.pincode.postalpincode-url:https://api.postalpincode.in/pincode}")
    private String postalPincodeUrl;

    @Value("${prospectsoul.pincode.nominatim-url:https://nominatim.openstreetmap.org/search}")
    private String nominatimUrl;

    @Value("${prospectsoul.pincode.timeout-seconds:15}")
    private int timeoutSeconds;

    @Value("${prospectsoul.pincode.user-agent:ProspectSoul/1.0 (contact@vyoog.com)}")
    private String userAgent;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public IndiaPincodeGeocoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<GeocodedPincode> geocode(String pincode) {
        if (pincode == null || !pincode.matches("\\d{6}")) return Optional.empty();

        // Ask India Post for metadata (area / district / state) and
        // Nominatim for coordinates. Both are used to pin down the
        // pincode; either alone is not enough.
        PostOfficeMetadata meta = fetchPostalMetadata(pincode);

        // Primary Nominatim query — direct postcode match. Nominatim
        // returns {addresstype: "postcode"} when it actually resolved
        // the pincode rather than fuzzy-matching an unrelated town.
        Optional<double[]> coords = geocodeWithNominatim(pincode + ", India", pincode, true);

        // Secondary: use the area/district/state we got from India Post
        // to pin down the coord when the direct pincode lookup missed.
        // Any result is acceptable here since we already know the
        // pincode is real (India Post confirmed it).
        if (coords.isEmpty() && meta != null) {
            String q = String.format("%s, %s, %s, India", meta.area, meta.district, meta.state);
            coords = geocodeWithNominatim(q, pincode, false);
        }

        if (coords.isEmpty()) {
            log.warn("geocode: no coords for pincode {}", pincode);
            return Optional.empty();
        }
        double[] latLng = coords.get();
        return Optional.of(new GeocodedPincode(
                pincode,
                meta != null ? meta.area : ("Pincode " + pincode),
                meta != null ? meta.district : null,
                meta != null ? meta.state    : null,
                BigDecimal.valueOf(latLng[0]),
                BigDecimal.valueOf(latLng[1]),
                meta != null ? "india_post+nominatim" : "nominatim"
        ));
    }

    @Override
    public String name() { return "india_post_nominatim"; }

    // --- postalpincode.in ---

    private record PostOfficeMetadata(String area, String district, String state) {}

    private PostOfficeMetadata fetchPostalMetadata(String pincode) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(postalPincodeUrl + "/" + pincode))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("user-agent", userAgent)
                    .header("accept", "application/json")
                    .GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) return null;

            JsonNode root = objectMapper.readTree(resp.body());
            // Response is an array with one element containing Status + PostOffice[].
            JsonNode envelope = root.isArray() && root.size() > 0 ? root.get(0) : root;
            String status = envelope.path("Status").asText("");
            if (!"Success".equalsIgnoreCase(status)) return null;
            JsonNode postOffices = envelope.path("PostOffice");
            if (!postOffices.isArray() || postOffices.isEmpty()) return null;
            JsonNode po = postOffices.get(0);
            return new PostOfficeMetadata(
                    po.path("Name").asText(""),
                    po.path("District").asText(""),
                    po.path("State").asText("")
            );
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.debug("postalpincode.in lookup failed for {}: {}", pincode, e.toString());
            return null;
        }
    }

    // --- Nominatim ---

    private Optional<double[]> geocodeWithNominatim(String query, String pincode,
                                                     boolean requirePostcodeMatch) {
        String url = nominatimUrl + "?format=json&countrycodes=in&limit=1&addressdetails=0"
                + "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("user-agent", userAgent)
                    .header("accept", "application/json")
                    .GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                log.debug("nominatim {} -> {}", url, resp.statusCode());
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(resp.body());
            if (!root.isArray() || root.isEmpty()) return Optional.empty();
            JsonNode first = root.get(0);

            // When we sent a bare-pincode query we insist Nominatim returns
            // a real postcode-type match — otherwise it fuzzy-matches to
            // any hamlet that happens to contain those digits and we
            // silently accept a wrong location.
            if (requirePostcodeMatch) {
                String at = first.path("addresstype").asText("");
                String tp = first.path("type").asText("");
                String name = first.path("name").asText("");
                boolean looksLikePostcode = "postcode".equalsIgnoreCase(at)
                        || "postcode".equalsIgnoreCase(tp)
                        || name.equals(pincode);
                if (!looksLikePostcode) {
                    log.debug("nominatim: rejected non-postcode match for {} (addresstype={}, type={}, name={})",
                            pincode, at, tp, name);
                    return Optional.empty();
                }
            }

            double lat = first.path("lat").asDouble(Double.NaN);
            double lng = first.path("lon").asDouble(Double.NaN);
            if (Double.isNaN(lat) || Double.isNaN(lng)) return Optional.empty();
            return Optional.of(new double[] {lat, lng});
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.debug("nominatim lookup failed for {}: {}", pincode, e.toString());
            return Optional.empty();
        }
    }
}
