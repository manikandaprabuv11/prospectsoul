package com.vyoog.prospectsoul_backend.enrichment.google;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
public class GooglePlacesClient {

    private static final String TEXT_SEARCH_URL = "https://places.googleapis.com/v1/places:searchText";
    private static final String PLACE_DETAILS_URL = "https://places.googleapis.com/v1/places/";
    private static final String FIELD_MASK = "places.id,places.displayName,places.formattedAddress," +
            "places.internationalPhoneNumber,places.websiteUri,places.types," +
            "places.googleMapsUri,places.location,places.businessStatus";
    private static final String DETAILS_FIELD_MASK = "id,displayName,formattedAddress," +
            "internationalPhoneNumber,websiteUri,types,googleMapsUri,location,businessStatus";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GooglePlacesClient(
            @Value("${prospectsoul.enrichment.google-places.api-key:}") String apiKey,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public JsonNode textSearch(String query) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GOOGLE_PLACES_API_KEY not configured");
        }

        String body = objectMapper.writeValueAsString(Map.of("textQuery", query));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TEXT_SEARCH_URL))
                .header("Content-Type", "application/json")
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", FIELD_MASK)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429) {
            throw new GooglePlacesRateLimitException("Google Places rate limit exceeded");
        }
        if (response.statusCode() >= 500) {
            throw new GooglePlacesTransientException("Google Places server error: " + response.statusCode());
        }
        if (response.statusCode() >= 400) {
            throw new GooglePlacesPermanentException("Google Places error: " + response.statusCode() + " " + response.body());
        }

        return objectMapper.readTree(response.body());
    }

    public JsonNode getPlaceDetails(String placeId) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GOOGLE_PLACES_API_KEY not configured");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PLACE_DETAILS_URL + placeId))
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", DETAILS_FIELD_MASK)
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429) {
            throw new GooglePlacesRateLimitException("Google Places rate limit exceeded");
        }
        if (response.statusCode() >= 500) {
            throw new GooglePlacesTransientException("Google Places server error: " + response.statusCode());
        }
        if (response.statusCode() >= 400) {
            throw new GooglePlacesPermanentException("Google Places error: " + response.statusCode() + " " + response.body());
        }

        return objectMapper.readTree(response.body());
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public static class GooglePlacesRateLimitException extends RuntimeException {
        public GooglePlacesRateLimitException(String message) { super(message); }
    }

    public static class GooglePlacesTransientException extends RuntimeException {
        public GooglePlacesTransientException(String message) { super(message); }
    }

    public static class GooglePlacesPermanentException extends RuntimeException {
        public GooglePlacesPermanentException(String message) { super(message); }
    }
}
