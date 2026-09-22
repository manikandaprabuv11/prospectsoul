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
                e.getCreatedBy(),
                e.getCreatedAt(),
                e.getUpdatedBy(),
                e.getUpdatedAt()
        );
    }
}
