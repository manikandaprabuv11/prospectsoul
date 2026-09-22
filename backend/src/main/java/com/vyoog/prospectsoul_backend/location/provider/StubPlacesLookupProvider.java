package com.vyoog.prospectsoul_backend.location.provider;

import java.math.BigDecimal;
import java.util.List;

import com.vyoog.prospectsoul_backend.location.dto.response.ExternalPlacesResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default provider — returns nothing. Mirrors the same "no live call on
 * CI" pattern as the phone-verification stub. Wire a real Google Places
 * provider by annotating it with {@code @Primary} in production config.
 */
@Component
@ConditionalOnProperty(
        prefix = "prospectsoul.places",
        name = "provider",
        havingValue = "stub")
public class StubPlacesLookupProvider implements PlacesLookupProvider {

    @Override
    public List<ExternalPlacesResponse.Result> search(String pincode, int radiusMeters,
                                                       String keyword, String type) {
        // Deterministic empty result so integration tests can assert
        // "no writes to companies" and "no google roundtrip."
        return List.of(new ExternalPlacesResponse.Result(
                "stub-" + pincode + "-1",
                "Stub Business " + pincode,
                "Stub Address near " + pincode,
                null,
                BigDecimal.ZERO, BigDecimal.ZERO,
                "OPERATIONAL", List.of("establishment")
        ));
    }

    @Override
    public String providerName() { return "stub"; }
}
