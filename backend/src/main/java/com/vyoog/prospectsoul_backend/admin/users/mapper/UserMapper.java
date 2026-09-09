package com.vyoog.prospectsoul_backend.admin.users.mapper;

import java.util.Map;

import com.vyoog.prospectsoul_backend.admin.users.dto.response.UserResponse;
import com.vyoog.prospectsoul_backend.admin.users.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    private static final Map<String, String> ROLE_DISPLAY_NAMES = Map.of(
            "PS_ANALYST", "Research Analyst",
            "PS_SALES_LEAD", "Sales Lead",
            "PS_ADMIN", "Administrator",
            "PS_VIEWER", "Viewer",
            "PS_COO", "COO"
    );

    public UserResponse toResponse(UserEntity entity) {
        return new UserResponse(
                entity.getId(),
                entity.getUsername(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getRole(),
                ROLE_DISPLAY_NAMES.getOrDefault(entity.getRole(), entity.getRole()),
                entity.isActive(),
                entity.getLastLoginAt(),
                entity.getCreatedAt()
        );
    }
}
