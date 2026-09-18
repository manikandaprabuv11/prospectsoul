package com.vyoog.prospectsoul_backend.verification.dto.response;

import java.time.Instant;
import java.util.UUID;

/** One company's verification result inside a batch. */
public record VerificationBatchItemResponse(
        UUID id,
        UUID batchId,
        UUID companyId,
        String companyName,
        String status,
        String phoneNumber,
        String normalizedPhoneNumber,
        String provider,
        String providerReference,
        Boolean phoneValid,
        String lineType,
        String carrierName,
        String mobileCountryCode,
        String mobileNetworkCode,
        String failureCode,
        String failureMessage,
        int attemptCount,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt
) {}
