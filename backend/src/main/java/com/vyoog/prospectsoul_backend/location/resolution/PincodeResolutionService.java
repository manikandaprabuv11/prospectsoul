package com.vyoog.prospectsoul_backend.location.resolution;

import java.math.BigDecimal;
import java.util.Optional;

import com.vyoog.prospectsoul_backend.common.exception.ResourceNotFoundException;
import com.vyoog.prospectsoul_backend.location.entity.PincodeCentroid;
import com.vyoog.prospectsoul_backend.location.repository.PincodeCentroidRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single entry point for "give me the centroid for this pincode".
 *
 * Semantics per the ProspectSoul spec discussion (Sales-Intelligence
 * follow-up, "make it dynamic"):
 *
 *   1. Read from {@code pincode_centroids} — the persistent cache and
 *      source of truth.
 *   2. On miss, ask a {@link PincodeGeocoder}. Persist the result via
 *      {@code INSERT ... ON CONFLICT DO UPDATE} so concurrent resolvers of
 *      the same pincode never race — the second writer sees the first
 *      writer's row and simply overwrites with the same value.
 *   3. If the geocoder also returns nothing, the pincode is treated as
 *      unknown — the caller surfaces a 404 with an actionable message.
 *
 * The service does NOT throw on a stale-cache row (e.g. an earlier
 * placeholder saved with India-centroid coords): {@link #resolve(String)}
 * always returns whatever is stored. A separate {@link #refresh(String)}
 * method exists for admins or a scheduled sweeper to upgrade placeholders,
 * but the map flow does not use it automatically to keep the request
 * latency bounded.
 */
@Service
@Slf4j
public class PincodeResolutionService {

    private final PincodeCentroidRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final PincodeGeocoder geocoder;

    public PincodeResolutionService(PincodeCentroidRepository repository,
                                     JdbcTemplate jdbcTemplate,
                                     PincodeGeocoder geocoder) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.geocoder = geocoder;
    }

    /**
     * Cache-first read. Returns a stored row when one exists (even a
     * placeholder), else calls the geocoder and persists the result.
     * Throws {@link ResourceNotFoundException} when the pincode is
     * genuinely unknown.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PincodeCentroid resolve(String pincode) {
        validate(pincode);

        Optional<PincodeCentroid> cached = repository.findByPincode(pincode);
        if (cached.isPresent()) return cached.get();

        Optional<PincodeGeocoder.GeocodedPincode> resolved = safeGeocode(pincode);
        if (resolved.isEmpty()) {
            throw new ResourceNotFoundException("Pincode " + pincode
                    + " could not be resolved (unknown to India Post and OpenStreetMap)");
        }
        return upsert(resolved.get());
    }

    /**
     * Force a fresh geocode and overwrite the stored row. Used to upgrade
     * a placeholder (e.g. one that landed at India-centroid because the
     * geocoder had a bad day). Never throws when the geocoder returns
     * empty — it just keeps the existing row.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PincodeCentroid refresh(String pincode) {
        validate(pincode);
        Optional<PincodeGeocoder.GeocodedPincode> resolved = safeGeocode(pincode);
        if (resolved.isEmpty()) {
            return repository.findByPincode(pincode).orElseThrow(
                    () -> new ResourceNotFoundException("Pincode " + pincode));
        }
        return upsert(resolved.get());
    }

    /**
     * Non-throwing accessor used by callers that need "the coordinates if
     * we have them, else nothing". Never triggers a geocoding call. The
     * map service uses this so a page load never blocks on the external
     * network — {@link #resolve} is what the user's search box calls.
     */
    @Transactional(readOnly = true)
    public Optional<PincodeCentroid> peek(String pincode) {
        if (pincode == null || !pincode.matches("\\d{6}")) return Optional.empty();
        return repository.findByPincode(pincode);
    }

    /** Concurrency-safe upsert. PK on pincode acts as the unique constraint. */
    PincodeCentroid upsert(PincodeGeocoder.GeocodedPincode g) {
        jdbcTemplate.update("""
                INSERT INTO pincode_centroids
                    (pincode, area_name, district, state, latitude, longitude, created_at)
                VALUES (?, ?, ?, ?, ?, ?, now())
                ON CONFLICT (pincode) DO UPDATE SET
                    area_name = EXCLUDED.area_name,
                    district  = EXCLUDED.district,
                    state     = EXCLUDED.state,
                    latitude  = EXCLUDED.latitude,
                    longitude = EXCLUDED.longitude
                """,
                g.pincode(),
                nz(g.areaName(), "Pincode " + g.pincode()),
                g.district(),
                g.state(),
                g.latitude(),
                g.longitude()
        );
        log.info("pincode resolver: cached {} ({}, {}) via {}",
                g.pincode(), g.latitude(), g.longitude(), g.source());
        return repository.findByPincode(g.pincode()).orElseThrow(
                () -> new IllegalStateException("upsert of pincode " + g.pincode() + " vanished"));
    }

    private Optional<PincodeGeocoder.GeocodedPincode> safeGeocode(String pincode) {
        try {
            return geocoder.geocode(pincode);
        } catch (RuntimeException e) {
            log.warn("pincode geocoder threw for {}: {}", pincode, e.toString());
            return Optional.empty();
        }
    }

    private void validate(String pincode) {
        if (pincode == null || !pincode.matches("\\d{6}")) {
            throw new IllegalArgumentException("pincode must be a 6-digit string");
        }
    }

    private String nz(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }

    // Test helper so integration tests can seed a deterministic centroid without
    // going through the geocoder.
    public PincodeCentroid seed(String pincode, BigDecimal lat, BigDecimal lng,
                                 String areaName, String district, String state) {
        return upsert(new PincodeGeocoder.GeocodedPincode(
                pincode, areaName, district, state, lat, lng, "seed"));
    }
}
