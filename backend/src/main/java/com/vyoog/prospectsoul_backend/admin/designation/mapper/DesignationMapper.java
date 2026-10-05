package com.vyoog.prospectsoul_backend.admin.designation.mapper;

import com.vyoog.prospectsoul_backend.admin.designation.dto.response.DesignationResponse;
import com.vyoog.prospectsoul_backend.admin.designation.entity.DecisionMakerDesignation;
import org.springframework.stereotype.Component;

@Component
public class DesignationMapper {

    public DesignationResponse toResponse(DecisionMakerDesignation entity) {
        return new DesignationResponse(
                entity.getId(),
                entity.getDesignation(),
                entity.getAliases(),
                entity.getActive(),
                entity.getCreatedAt()
        );
    }
}
