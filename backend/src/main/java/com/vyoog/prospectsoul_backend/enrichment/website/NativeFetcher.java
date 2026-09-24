package com.vyoog.prospectsoul_backend.enrichment.website;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NativeFetcher {

    private static final int FETCH_TIMEOUT_MS = 10_000;
    private static final String USER_AGENT = "VyoogBot/1.0";

    private final HttpClient httpClient;

    public NativeFetcher() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public FetchResult fetch(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .timeout(Duration.ofMillis(FETCH_TIMEOUT_MS))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return new FetchResult(true, response.body(), response.statusCode(), null);
            }
            return new FetchResult(false, null, response.statusCode(), "HTTP " + response.statusCode());
        } catch (Exception e) {
            log.debug("Native fetch failed for {}: {}", url, e.getMessage());
            return new FetchResult(false, null, 0, e.getMessage());
        }
    }

    public record FetchResult(boolean success, String body, int statusCode, String error) {}
}
