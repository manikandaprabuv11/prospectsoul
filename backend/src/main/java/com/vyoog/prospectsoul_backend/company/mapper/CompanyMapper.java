package com.vyoog.prospectsoul_backend.company.mapper;

import com.vyoog.prospectsoul_backend.company.dto.response.CompanyResponse;
import com.vyoog.prospectsoul_backend.company.entity.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanyResponse toResponse(Company entity) {
        return new CompanyResponse(
                entity.getId(),
                entity.getCanonicalName(),
                entity.getNormalizedName(),
                entity.getWebsiteDomain(),
                entity.getPrimaryPhoneNormalized(),
                entity.getEmail(),
                entity.getCity(),
                entity.getState(),
                entity.getCluster(),
                entity.getIndustry(),
                entity.getSizeBand(),
                entity.getTags(),
                entity.getSource(),
                entity.getPipelineState().name(),
                entity.getCompletenessScore(),
                entity.getVerificationStatus().name(),
                entity.getVerifiedBy(),
                entity.getVerifiedAt(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt()
        );
    }
}
