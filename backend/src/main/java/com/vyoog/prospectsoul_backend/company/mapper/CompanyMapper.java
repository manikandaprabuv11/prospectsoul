package com.vyoog.prospectsoul_backend.company.mapper;

import com.vyoog.prospectsoul_backend.company.dto.response.CompanyResponse;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanyResponse toResponse(Company e) {
        return new CompanyResponse(
                e.getId(),
                e.getCanonicalName(),
                e.getNormalizedName(),
                e.getWebsiteDomain(),
                e.getPrimaryPhoneNormalized(),
                e.getEmail(),
                e.getCity(),
                e.getState(),
                e.getCluster(),
                e.getIndustry(),
                e.getSizeBand(),
                e.getTags(),
                e.getSource(),
                e.getPipelineState().name(),
                e.getCompletenessScore(),
                e.getVerificationStatus().name(),
                e.getVerifiedBy(),
                e.getVerifiedAt(),
                e.getPincode(),
                e.getDistrict(),
                e.getAddressLine(),
                e.getRegion(),
                e.getProducts(),
                e.getTurnover(),
                e.getGstNumber(),
                e.getEmployeeCount(),
                e.getRegistrationDate(),
                e.getSourceReference(),
                e.getLgStateCode(),
                e.getLgDistrictCode(),
                e.getPrimaryNicCodeId(),
                e.getLatitude(),
                e.getLongitude(),
                // Enrichment: Google Places
                e.getGooglePlaceId(),
                e.getGoogleName(),
                e.getGoogleBusinessCategory(),
                e.getGoogleBusinessTypes(),
                e.getGoogleMapsUrl(),
                e.getGoogleLat(),
                e.getGoogleLng(),
                e.getGoogleBusinessStatus(),
                e.getGoogleLastEnrichedAt(),
                // Enrichment: Website
                e.getWebsiteReachable(),
                e.getWebsiteTitle(),
                e.getWebsiteDescription(),
                e.getWebsiteLastEnrichedAt(),
                // Enrichment: Social
                e.getSocialLinkedin(),
                e.getSocialFacebook(),
                e.getSocialX(),
                e.getSocialInstagram(),
                e.getSocialYoutube(),
                // Enrichment: Phone
                e.getPrimaryPhoneCountry(),
                e.getPrimaryPhoneRegion(),
                e.getPrimaryPhoneCarrier(),
                e.getPrimaryPhoneType(),
                e.getPrimaryPhoneStatus(),
                e.getPrimaryPhoneDndRegistered(),
                e.getPrimaryPhoneLastEnrichedAt(),
                // List-only enriched
                null, null, null, null,
                e.getCreatedBy(),
                e.getCreatedAt(),
                e.getUpdatedBy(),
                e.getUpdatedAt()
        );
    }
}
