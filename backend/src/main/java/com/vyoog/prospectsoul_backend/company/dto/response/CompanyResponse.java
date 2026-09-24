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
        // Enriched fields used on the Companies list — populated only when
        // returned from the paginated list endpoint. Detail endpoint leaves
        // them null.
        // Enrichment: Google Places
        String googlePlaceId,
        String googleName,
        String googleBusinessCategory,
        String googleBusinessTypes,
        String googleMapsUrl,
        BigDecimal googleLat,
        BigDecimal googleLng,
        String googleBusinessStatus,
        Instant googleLastEnrichedAt,
        // Enrichment: Website
        Boolean websiteReachable,
        String websiteTitle,
        String websiteDescription,
        Instant websiteLastEnrichedAt,
        // Enrichment: Social
        String socialLinkedin,
        String socialFacebook,
        String socialX,
        String socialInstagram,
        String socialYoutube,
        // Enrichment: Phone
        String primaryPhoneCountry,
        String primaryPhoneRegion,
        String primaryPhoneCarrier,
        String primaryPhoneType,
        String primaryPhoneStatus,
        Boolean primaryPhoneDndRegistered,
        Instant primaryPhoneLastEnrichedAt,
        // List-only enriched fields
        String primaryContactName,
        String primaryContactPhone,
        String primaryContactRole,
        java.util.List<NicCodeRef> nicCodes,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt
) {
    public record NicCodeRef(String code, String description, boolean primary) {}
}
