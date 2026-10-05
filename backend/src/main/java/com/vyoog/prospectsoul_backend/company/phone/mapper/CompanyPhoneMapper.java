package com.vyoog.prospectsoul_backend.company.phone.mapper;

import com.vyoog.prospectsoul_backend.admin.designation.service.DecisionMakerDesignationService;
import com.vyoog.prospectsoul_backend.company.phone.dto.response.CompanyPhoneResponse;
import com.vyoog.prospectsoul_backend.company.phone.entity.CompanyPhone;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompanyPhoneMapper {

    private final DecisionMakerDesignationService designationService;

    public CompanyPhoneResponse toResponse(CompanyPhone phone) {
        String designation = phone.resolveDesignation();
        boolean isDm = designationService.isDecisionMaker(designation);
        String contactName = phone.getContact() != null ? phone.getContact().getName() : null;

        return new CompanyPhoneResponse(
                phone.getId(),
                phone.getNumberRaw(),
                phone.getNumberNormalized(),
                phone.getPhoneType(),
                phone.getNumberSource(),
                phone.getConfidence(),
                phone.getConfidenceMode(),
                designation,
                isDm,
                phone.getIsPrimary(),
                phone.getContact() != null ? phone.getContact().getId() : null,
                contactName,
                phone.getDesignationOverride(),
                phone.getOverrideReason(),
                phone.getEnrichedCountry(),
                phone.getEnrichedRegion(),
                phone.getEnrichedCarrier(),
                phone.getEnrichedLineType(),
                phone.getEnrichedStatus(),
                phone.getEnrichedDnd(),
                phone.getEnrichedAt(),
                phone.getCreatedAt(),
                phone.getCreatedBy()
        );
    }
}
