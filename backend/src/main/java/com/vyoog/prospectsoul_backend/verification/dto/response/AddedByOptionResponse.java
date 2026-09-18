package com.vyoog.prospectsoul_backend.verification.dto.response;

/**
 * One option of the "Added By" filter dropdown.
 *
 * @param id           the value stored in {@code companies.created_by}
 * @param name         display name resolved from the local user mirror, falling
 *                     back to the raw id when the actor is not a known user
 * @param companyCount how many companies that actor added, so the analyst can
 *                     see which actors are worth filtering by
 */
public record AddedByOptionResponse(
        String id,
        String name,
        long companyCount
) {}
