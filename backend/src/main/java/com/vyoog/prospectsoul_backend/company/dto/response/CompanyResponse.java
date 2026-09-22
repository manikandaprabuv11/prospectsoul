package com.vyoog.prospectsoul_backend.company.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String canonicalName,
        String normalizedName,
        String websiteDomain,
        String primaryPhoneNormalized,
        String email,
        String city,
        String state,
        String cluster,
        String industry,
        String sizeBand,
        List<String> tags,
        String source,
        String pipelineState,
        Integer completenessScore,
        String verificationStatus,
        String verifiedBy,
        Instant verifiedAt,
        // Sales-Intelligence extension (docs 21 §4, ADR-0003).
        String pincode,
        String district,
        String addressLine,
        String region,
        String products,
        BigDecimal turnover,
        String gstNumber,
        Integer employeeCount,
        LocalDate registrationDate,
        String sourceReference,
        Short lgStateCode,
        Integer lgDistrictCode,
        UUID primaryNicCodeId,
        BigDecimal latitude,
        BigDecimal longitude,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt
) {}
