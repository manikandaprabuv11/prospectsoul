package com.vyoog.prospectsoul_backend.location.resolution;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Adapter for external geocoding of an Indian pincode. Returns a
 * {@link GeocodedPincode} on success, {@link Optional#empty()} when the
 * pincode is genuinely unknown, and throws when the upstream is down
 * (surfaced as 502 to the caller).
 *
 * Implementations must be side-effect free — they don't write to the
 * database. Caching + persistence is the {@link PincodeResolutionService}'s
 * job, so a single adapter can be reused from every caller (map, external
 * places search, admin lookup).
 */
public interface PincodeGeocoder {

    Optional<GeocodedPincode> geocode(String pincode);

    String name();

    record GeocodedPincode(
            String pincode,
            String areaName,
            String district,
            String state,
            BigDecimal latitude,
            BigDecimal longitude,
            String source
    ) {}
}
