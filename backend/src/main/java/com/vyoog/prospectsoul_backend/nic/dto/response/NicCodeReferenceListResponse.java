package com.vyoog.prospectsoul_backend.nic.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Payload for the 422 body returned when an in-use NIC code is deactivated
 * without the {@code force_deactivate} flag. Lists the companies that would
 * be affected — bounded so the response stays small.
 */
public record NicCodeReferenceListResponse(
        UUID nicCodeId,
        long affectedCompanyCount,
        List<UUID> sampleCompanyIds
) {}
