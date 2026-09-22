package com.vyoog.prospectsoul_backend.location.resolution;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test-only pincode geocoder. Deterministic, in-process, records every
 * call so the tests can assert that a cached hit skips the network path.
 */
@TestConfiguration
public class TestPincodeGeocoderConfig {

    public static class RecordingStub implements PincodeGeocoder {
        // Small fixture set — the tests can add to this per-method via
        // fixtures().put(...). Any pincode not present resolves to empty
        // (the resolver then throws 404).
        public final Map<String, GeocodedPincode> fixtures = new ConcurrentHashMap<>();
        public final AtomicInteger callCount = new AtomicInteger();

        @Override
        public Optional<GeocodedPincode> geocode(String pincode) {
            callCount.incrementAndGet();
            return Optional.ofNullable(fixtures.get(pincode));
        }

        @Override
        public String name() { return "stub"; }

        public RecordingStub with(String pincode, double lat, double lng,
                                   String area, String district, String state) {
            fixtures.put(pincode, new GeocodedPincode(pincode, area, district, state,
                    BigDecimal.valueOf(lat), BigDecimal.valueOf(lng), "stub"));
            return this;
        }
    }

    @Bean
    @Primary
    public PincodeGeocoder testPincodeGeocoder() {
        return new RecordingStub();
    }
}
