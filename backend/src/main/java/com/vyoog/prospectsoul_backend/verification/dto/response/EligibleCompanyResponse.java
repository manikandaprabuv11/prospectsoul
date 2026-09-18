package com.vyoog.prospectsoul_backend.verification.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * A row of the "Verify New Companies" selection table.
 *
 * @param inFlight true when this company already has a queued or processing
 *                 item in another batch; the row must not be selectable
 * @param phoneUsable false when the stored phone cannot be turned into a valid
 *                    E.164 number, so the analyst can see up front that the
 *                    row would be skipped with NO_PHONE
 */
public record EligibleCompanyResponse(
        UUID id,
        String canonicalName,
        String city,
        String state,
        String primaryPhoneNormalized,
        String verificationStatus,
        String addedBy,
        String addedByName,
        Instant addedAt,
        boolean phoneUsable,
        boolean inFlight
) {}
