package com.vyoog.prospectsoul_backend.verification.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A verification batch as the Verify page renders it — the progress card, the
 * history row and the job detail header all read this shape.
 *
 * @param requestedByName     display name resolved from the local user mirror,
 *                            so the UI never has to show a raw subject id
 * @param progressPercent     terminal items as a percentage of the total
 * @param elapsedSeconds      seconds from {@code startedAt} to completion, or
 *                            to now while the batch is still running
 */
public record VerificationBatchResponse(
        UUID id,
        String status,
        String requestedBy,
        String requestedByName,
        String filterAddedBy,
        String filterAddedByName,
        LocalDate filterDateFrom,
        LocalDate filterDateTo,
        int totalCount,
        int queuedCount,
        int processingCount,
        int verifiedCount,
        int failedCount,
        int skippedCount,
        int progressPercent,
        Long elapsedSeconds,
        CurrentItem currentItem,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * The company currently being looked up, for the "Currently verifying"
     * line of the progress card. Null unless an item is PROCESSING.
     */
    public record CurrentItem(
            UUID companyId,
            String companyName,
            String phoneNumber,
            String provider
    ) {}
}
