package com.vyoog.prospectsoul_backend.verification.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * A row of the Verify page's bottom table. Only companies whose canonical
 * verification status is VERIFIED are ever returned here (docs/dev_docs/13
 * §11, and the core invariant of docs/dev_docs/17 §1).
 *
 * <p>{@code lineType} and {@code carrierName} come from the most recent
 * successful verification item for the company, which is why an entry can be
 * VERIFIED with those fields empty: a company verified through the pre-existing
 * manual {@code POST /api/v1/companies/{id}/verify} never went through a
 * provider lookup.
 */
public record VerifiedCompanyResponse(
        UUID id,
        String canonicalName,
        String city,
        String state,
        String verifiedBy,
        String verifiedByName,
        Instant verifiedAt,
        String addedBy,
        String addedByName,
        Instant addedAt,
        String verifiedPhone,
        String lineType,
        String carrierName,
        String provider
) {}
