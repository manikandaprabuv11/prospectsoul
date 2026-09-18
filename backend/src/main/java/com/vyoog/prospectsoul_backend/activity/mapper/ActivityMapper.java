package com.vyoog.prospectsoul_backend.activity.mapper;

import com.vyoog.prospectsoul_backend.activity.dto.response.ActivityResponse;
import com.vyoog.prospectsoul_backend.activity.entity.Activity;
import org.springframework.stereotype.Component;

@Component
public class ActivityMapper {

    public ActivityResponse toResponse(Activity entity) {
        return new ActivityResponse(
                entity.getId(),
                entity.getCompanyId(),
                entity.getType().name(),
                entity.getContent(),
                Boolean.TRUE.equals(entity.getVerified()),
                entity.getCreatedBy(),
                entity.getCreatedAt()
        );
    }
}
