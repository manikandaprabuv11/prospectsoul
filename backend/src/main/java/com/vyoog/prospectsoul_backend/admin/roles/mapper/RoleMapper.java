package com.vyoog.prospectsoul_backend.admin.roles.mapper;

import java.util.List;

import com.vyoog.prospectsoul_backend.admin.roles.dto.response.RoleResponse;
import com.vyoog.prospectsoul_backend.admin.roles.entity.RoleEntity;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class RoleMapper {

    private final ObjectMapper objectMapper;

    public RoleMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RoleResponse toResponse(RoleEntity entity, long userCount) {
        return new RoleResponse(
                entity.getId(),
                entity.getName(),
                entity.getDisplayName(),
                entity.getDescription(),
                parsePermissions(entity.getPermissions()),
                userCount,
                entity.isActive()
        );
    }

    @SuppressWarnings("unchecked")
    private List<String> parsePermissions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }
}
