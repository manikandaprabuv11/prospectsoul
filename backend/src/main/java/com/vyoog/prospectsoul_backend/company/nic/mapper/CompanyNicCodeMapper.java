package com.vyoog.prospectsoul_backend.company.nic.mapper;

import com.vyoog.prospectsoul_backend.company.nic.dto.response.CompanyNicCodeResponse;
import com.vyoog.prospectsoul_backend.company.nic.entity.CompanyNicCode;
import org.springframework.stereotype.Component;

@Component
public class CompanyNicCodeMapper {
    public CompanyNicCodeResponse toResponse(CompanyNicCode e) {
        return new CompanyNicCodeResponse(
                e.getId(),
                e.getCompanyId(),
                e.getNicCodeId(),
                e.getNicCodeRaw(),
                e.getNicCode() == null ? null : e.getNicCode().getCode(),
                e.getNicCode() == null ? null : e.getNicCode().getDescription(),
                e.getDescriptionRaw(),
                e.getIsPrimary(),
                e.getSequenceNo(),
                e.getCreatedAt()
        );
    }
}
