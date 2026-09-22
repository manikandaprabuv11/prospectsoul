package com.vyoog.prospectsoul_backend.location.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Response for {@code GET /api/v1/map/companies?pincode=&radius_km=}.
 * Companies are placed at their own latitude/longitude when set; otherwise
 * they collapse to the pincode centroid — Domain Model Addendum
 * §Invariant 15 (external results transient) does not apply because these
 * are OWNED companies. Tier is deliberately omitted until the qualification
 * module ships.
 */
public record MapCompanyResponse(
        Centre center,
        double radiusKm,
        boolean unknownPincode,
        List<Item> content
) {
    public record Centre(BigDecimal lat, BigDecimal lng) {}
    public record Item(
            UUID id,
            String canonicalName,
            String pipelineState,
            BigDecimal lat,
            BigDecimal lng,
            UUID primaryNicCodeId
    ) {}
}
