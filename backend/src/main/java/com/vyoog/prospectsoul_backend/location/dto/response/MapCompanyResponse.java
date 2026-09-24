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
        List<Item> content,
        // Full set of NIC code ids the active nic_parent_id filter matched
        // (the parent id itself plus its descendants when
        // nic_include_descendants was honoured) — null when no NIC filter
        // was supplied. The Companies Map page uses this, together with
        // each item's own (always-unfiltered) nicCodes, to show only the
        // NIC chips that matched the active filter without baking that
        // display decision into this endpoint's data shape.
        List<UUID> matchedNicCodeIds
) {
    public record Centre(BigDecimal lat, BigDecimal lng) {}
    public record Item(
            UUID id,
            String canonicalName,
            String pipelineState,
            BigDecimal lat,
            BigDecimal lng,
            UUID primaryNicCodeId,
            List<NicCodeRef> nicCodes
    ) {}
    public record NicCodeRef(UUID id, String code, String description, boolean primary) {}
}
