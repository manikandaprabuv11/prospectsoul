package com.vyoog.prospectsoul_backend.verification.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

/**
 * Body of {@code POST /api/v1/verifications}.
 *
 * <p>The filter fields are not decoration: they are re-applied server-side, so
 * a company that does not match the Added By / date range the analyst selected
 * under cannot be verified by passing its id (docs/dev_docs/16, "Verify New
 * Companies"). They are also persisted on the batch as the filter snapshot the
 * history table shows.
 *
 * <p>JSON is snake_case ({@code company_ids}, {@code added_by},
 * {@code date_from}, {@code date_to}) via the application-wide naming strategy.
 */
public record StartVerificationRequest(
        @NotEmpty(message = "At least one company must be selected")
        List<UUID> companyIds,
        String addedBy,
        LocalDate dateFrom,
        LocalDate dateTo
) {}
