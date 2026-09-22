package com.vyoog.prospectsoul_backend.location.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Live external result payload. NEVER persisted (Domain Model Addendum
 * Invariant 15). The {@code persisted: false} field is set literally so a
 * consumer that inspects the payload can confirm the contract.
 */
public record ExternalPlacesResponse(
        List<Result> results,
        String source,
        boolean persisted,
        Integer quotaRemaining
) {
    public record Result(
            String placeId,
            String name,
            String formattedAddress,
            String phone,
            BigDecimal lat,
            BigDecimal lng,
            String businessStatus,
            List<String> types
    ) {}
}
