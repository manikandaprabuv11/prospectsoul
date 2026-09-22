package com.vyoog.prospectsoul_backend.location.provider;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * External Places provider backed by OpenStreetMap's Overpass API. Works
 * out of the box — no API key required — so the "Found externally" tab on
 * the map screen actually returns real, live businesses near the pincode
 * even before a Google Places key is provisioned.
 *
 * Activation order: {@link GooglePlacesLookupProvider} wins when
 * {@code GOOGLE_PLACES_API_KEY} is set; otherwise this Overpass provider
 * runs; the {@link StubPlacesLookupProvider} is only used when neither is
 * available (test contexts).
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "prospectsoul.places",
        name = "provider",
        havingValue = "overpass",
        matchIfMissing = true)
@ConditionalOnMissingBean(GooglePlacesLookupProvider.class)
public class OverpassPlacesLookupProvider implements PlacesLookupProvider {

    private final PincodeResolutionService pincodeResolver;
    private final ObjectMapper objectMapper;

    @Value("${prospectsoul.places.overpass-url:https://overpass-api.de/api/interpreter}")
    private String overpassUrl;

    @Value("${prospectsoul.places.overpass-timeout-seconds:20}")
    private int timeoutSeconds;

    @Value("${prospectsoul.places.max-results:30}")
    private int maxResults;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public List<ExternalPlacesResponse.Result> search(String pincode, int radiusMeters,
                                                       String keyword, String type) {
        // Overpass needs a centre — resolve on-demand (cached in
        // pincode_centroids). If the pincode is truly unknown, return
        // empty rather than throwing so the caller can still show an
        // empty external tab beside the owned companies list.
        PincodeCentroid centroid;
        try {
            centroid = pincodeResolver.resolve(pincode);
        } catch (RuntimeException e) {
            log.debug("overpass: cannot resolve pincode {} ({}) — returning empty",
                    pincode, e.toString());
            return List.of();
        }
        BigDecimal lat = centroid.getLatitude();
        BigDecimal lng = centroid.getLongitude();

        String query = buildOverpassQuery(lat, lng, radiusMeters, keyword, type);
        log.debug("overpass query: {}", query);

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(overpassUrl))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("user-agent", "ProspectSoul/1.0 (support@vyoog.com)")
                    .POST(HttpRequest.BodyPublishers.ofString("data=" +
                            java.net.URLEncoder.encode(query, StandardCharsets.UTF_8)))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                log.warn("overpass returned {} — body prefix: {}", resp.statusCode(),
                        resp.body().substring(0, Math.min(200, resp.body().length())));
                if (resp.statusCode() == 429 || resp.statusCode() == 504
                        || resp.statusCode() == 503) {
                    throw new BusinessRuleException(
                            "External Places API is rate-limiting us right now — please try "
                                    + "again in a few seconds. If this happens often, set the "
                                    + "GOOGLE_PLACES_API_KEY environment variable to switch to "
                                    + "Google Places (higher quota).");
                }
                throw new BusinessRuleException(
                        "External Places API returned " + resp.statusCode()
                                + " — please try again.");
            }
            return parseOverpassResponse(resp.body());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("overpass call failed", e);
            throw new BusinessRuleException("External lookup unavailable: " + e.getMessage());
        }
    }

    private String buildOverpassQuery(BigDecimal lat, BigDecimal lng, int radiusMeters,
                                       String keyword, String type) {
        // Match a broad set of "business" tags so the results resemble
        // Google Places' establishment list — shops, offices, healthcare,
        // banks, restaurants, industrial units.
        String filters;
        if (keyword != null && !keyword.isBlank()) {
            // Free-text keyword — match on the 'name' tag case-insensitively.
            String q = keyword.replace("\"", "\\\"");
            filters = String.format(
                    "  node[\"name\"~\"%s\", i](around:%d,%s,%s);%n"
                    + "  way[\"name\"~\"%s\", i](around:%d,%s,%s);",
                    q, radiusMeters, lat, lng, q, radiusMeters, lat, lng);
        } else {
            filters = String.format(
                      "  node[\"shop\"](around:%1$d,%2$s,%3$s);%n"
                    + "  node[\"office\"](around:%1$d,%2$s,%3$s);%n"
                    + "  node[\"industrial\"](around:%1$d,%2$s,%3$s);%n"
                    + "  node[\"craft\"](around:%1$d,%2$s,%3$s);%n"
                    + "  node[\"amenity\"~\"^(bank|restaurant|cafe|marketplace|pharmacy|clinic|hospital|fuel|car_rental)$\"](around:%1$d,%2$s,%3$s);%n"
                    + "  way[\"industrial\"](around:%1$d,%2$s,%3$s);%n"
                    + "  way[\"landuse\"=\"industrial\"](around:%1$d,%2$s,%3$s);",
                    radiusMeters, lat, lng);
        }

        return "[out:json][timeout:" + timeoutSeconds + "];\n"
                + "(\n" + filters + "\n);\n"
                + "out center " + maxResults + ";";
    }

    private List<ExternalPlacesResponse.Result> parseOverpassResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode elements = root.path("elements");
            if (!elements.isArray()) return List.of();
            List<ExternalPlacesResponse.Result> out = new ArrayList<>();
            for (JsonNode e : elements) {
                String id = e.path("type").asText("node") + "/" + e.path("id").asText("");
                JsonNode tags = e.path("tags");
                String name = tags.path("name").asText(null);
                if (name == null || name.isBlank()) continue; // skip unnamed nodes

                // way results carry {center: {lat,lon}}; node results carry top-level lat/lon
                double lat = e.path("lat").isMissingNode()
                        ? e.path("center").path("lat").asDouble()
                        : e.path("lat").asDouble();
                double lng = e.path("lon").isMissingNode()
                        ? e.path("center").path("lon").asDouble()
                        : e.path("lon").asDouble();

                String phone = firstNonBlank(
                        tags.path("phone").asText(null),
                        tags.path("contact:phone").asText(null));
                String address = composeAddress(tags);
                String status = "operational".equalsIgnoreCase(
                        tags.path("opening_hours").asText("")) ? "OPERATIONAL" : "UNKNOWN";

                List<String> types = new ArrayList<>();
                addIf(types, tags, "shop");
                addIf(types, tags, "office");
                addIf(types, tags, "amenity");
                addIf(types, tags, "craft");
                addIf(types, tags, "industrial");
                if (types.isEmpty()) types.add("establishment");

                out.add(new ExternalPlacesResponse.Result(
                        id, name, address, phone,
                        BigDecimal.valueOf(lat), BigDecimal.valueOf(lng),
                        status, types));
                if (out.size() >= maxResults) break;
            }
            return out;
        } catch (RuntimeException e) {
            log.warn("failed to parse overpass response", e);
            return List.of();
        }
    }

    private String composeAddress(JsonNode tags) {
        String street = tags.path("addr:street").asText("");
        String hn = tags.path("addr:housenumber").asText("");
        String city = tags.path("addr:city").asText("");
        String pin = tags.path("addr:postcode").asText("");
        StringBuilder sb = new StringBuilder();
        if (!hn.isBlank()) sb.append(hn).append(" ");
        if (!street.isBlank()) sb.append(street);
        if (!city.isBlank()) sb.append(sb.length() > 0 ? ", " : "").append(city);
        if (!pin.isBlank())  sb.append(sb.length() > 0 ? " "  : "").append(pin);
        return sb.length() == 0 ? "" : sb.toString();
    }

    private void addIf(List<String> out, JsonNode tags, String key) {
        String v = tags.path(key).asText(null);
        if (v != null && !v.isBlank()) out.add(key + "=" + v);
    }

    private String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }

    @Override
    public String providerName() { return "openstreetmap_overpass"; }
}
