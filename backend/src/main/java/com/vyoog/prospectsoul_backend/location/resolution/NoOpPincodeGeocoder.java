package com.vyoog.prospectsoul_backend.location.resolution;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Fallback geocoder that always returns empty. Used when no other
 * {@link PincodeGeocoder} bean is present — for example in a test context
 * that has not imported the stub config, or in a locked-down deployment
 * where the operator has set {@code prospectsoul.pincode.geocoder=disabled}.
 *
 * The resolver treats "empty" as "unknown pincode" and surfaces a 404, so
 * this bean preserves pre-refactor behaviour when no real geocoder is
 * wired.
 */
@Component
@ConditionalOnMissingBean(value = PincodeGeocoder.class,
        ignored = NoOpPincodeGeocoder.class)
public class NoOpPincodeGeocoder implements PincodeGeocoder {

    @Override
    public Optional<GeocodedPincode> geocode(String pincode) { return Optional.empty(); }

    @Override
    public String name() { return "noop"; }
}
