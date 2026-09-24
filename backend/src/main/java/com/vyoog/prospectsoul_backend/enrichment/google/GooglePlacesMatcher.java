package com.vyoog.prospectsoul_backend.enrichment.google;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
@Slf4j
public class GooglePlacesMatcher {

    private final GooglePlacesClient client;

    public record MatchResult(JsonNode place, String matchMethod, List<String> attemptLog) {}

    public MatchResult findBestMatch(String existingPlaceId, String canonicalName,
                                      String pincode, String city, String state) throws Exception {
        List<String> attemptLog = new ArrayList<>();

        if (existingPlaceId != null && !existingPlaceId.isBlank()) {
            log.debug("Refreshing existing place_id: {}", existingPlaceId);
            attemptLog.add("refresh_place_id:" + existingPlaceId);
            try {
                JsonNode details = client.getPlaceDetails(existingPlaceId);
                if (details != null && details.has("id")) {
                    return new MatchResult(details, "REFRESH", attemptLog);
                }
            } catch (Exception e) {
                log.warn("Failed to refresh place_id {}: {}", existingPlaceId, e.getMessage());
                attemptLog.add("refresh_failed:" + e.getMessage());
            }
        }

        if (pincode != null && !pincode.isBlank()) {
            String query = canonicalName + " " + pincode;
            attemptLog.add("text_search:name+pincode=" + query);
            JsonNode result = searchAndPickTop(query);
            if (result != null) {
                return new MatchResult(result, "NAME_PINCODE", attemptLog);
            }
        }

        if (city != null && !city.isBlank()) {
            String query = canonicalName + " " + city + (state != null ? ", " + state : "");
            attemptLog.add("text_search:name+city=" + query);
            JsonNode result = searchAndPickTop(query);
            if (result != null) {
                return new MatchResult(result, "NAME_CITY", attemptLog);
            }
        }

        {
            attemptLog.add("text_search:name_only=" + canonicalName);
            JsonNode result = searchAndPickTop(canonicalName);
            if (result != null) {
                return new MatchResult(result, "NAME_ONLY", attemptLog);
            }
        }

        attemptLog.add("no_match");
        return null;
    }

    private JsonNode searchAndPickTop(String query) throws Exception {
        JsonNode response = client.textSearch(query);
        JsonNode places = response.get("places");
        if (places == null || !places.isArray() || places.isEmpty()) {
            return null;
        }
        return places.get(0);
    }
}
