package com.vyoog.prospectsoul_backend.enrichment.website;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
public class FirecrawlClient {

    private static final String FIRECRAWL_API_URL = "https://api.firecrawl.dev/v1/scrape";
    private static final int TIMEOUT_MS = 25_000;

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public FirecrawlClient(
            @Value("${prospectsoul.enrichment.firecrawl.api-key:}") String apiKey,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public JsonNode scrape(String url) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("FIRECRAWL_API_KEY not configured");
        }

        String body = objectMapper.writeValueAsString(Map.of(
                "url", url,
                "formats", List.of("markdown"),
                "onlyMainContent", true,
                "timeout", TIMEOUT_MS
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FIRECRAWL_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            throw new FirecrawlPermanentException("Firecrawl auth failed (401)");
        }
        if (response.statusCode() == 402) {
            throw new FirecrawlPermanentException("Firecrawl credits exhausted (402)");
        }
        if (response.statusCode() == 429) {
            throw new FirecrawlTransientException("Firecrawl rate limit (429)");
        }
        if (response.statusCode() >= 500) {
            throw new FirecrawlTransientException("Firecrawl server error: " + response.statusCode());
        }
        if (response.statusCode() >= 400) {
            throw new FirecrawlPermanentException("Firecrawl error: " + response.statusCode() + " " + response.body());
        }

        return objectMapper.readTree(response.body());
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public static class FirecrawlTransientException extends RuntimeException {
        public FirecrawlTransientException(String message) { super(message); }
    }

    public static class FirecrawlPermanentException extends RuntimeException {
        public FirecrawlPermanentException(String message) { super(message); }
    }
}
