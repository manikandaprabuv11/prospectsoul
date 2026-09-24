package com.vyoog.prospectsoul_backend.company.defaultfilter.mapper;

import com.vyoog.prospectsoul_backend.company.defaultfilter.dto.response.CompanyDefaultFilterResponse;
import com.vyoog.prospectsoul_backend.company.defaultfilter.entity.CompanyDefaultFilter;
import org.springframework.stereotype.Component;

@Component
public class CompanyDefaultFilterMapper {
    public CompanyDefaultFilterResponse toResponse(CompanyDefaultFilter e) {
        return new CompanyDefaultFilterResponse(
                e.getId(), e.getFilterKey(), e.getLabel(), e.getOperator(),
                e.getValue(), e.getActive(), e.getSortOrder(),
                e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedBy(), e.getUpdatedAt());
    }
}
