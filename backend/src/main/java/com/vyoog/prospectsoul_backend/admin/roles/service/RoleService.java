package com.vyoog.prospectsoul_backend.admin.roles.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.admin.roles.dto.request.RoleUpdateRequest;
import com.vyoog.prospectsoul_backend.admin.roles.dto.response.RoleDetailResponse;
import com.vyoog.prospectsoul_backend.admin.roles.dto.response.RoleResponse;
import com.vyoog.prospectsoul_backend.admin.roles.entity.RoleEntity;
import com.vyoog.prospectsoul_backend.admin.roles.mapper.RoleMapper;
import com.vyoog.prospectsoul_backend.admin.roles.repository.RoleRepository;
import tools.jackson.databind.ObjectMapper;
import com.vyoog.prospectsoul_backend.admin.users.dto.response.UserResponse;
import com.vyoog.prospectsoul_backend.admin.users.entity.UserEntity;
import com.vyoog.prospectsoul_backend.admin.users.entity.UserRoleEntity;
import com.vyoog.prospectsoul_backend.admin.users.mapper.UserMapper;
import com.vyoog.prospectsoul_backend.admin.users.repository.UserRepository;
import com.vyoog.prospectsoul_backend.admin.users.repository.UserRoleRepository;
import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.common.exception.BusinessRuleException;
import com.vyoog.prospectsoul_backend.common.exception.ConflictException;
import com.vyoog.prospectsoul_backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleMapper roleMapper;
    private final UserMapper userMapper;
    private final AuditService auditService;
    private final Keycloak keycloak;
    private final ObjectMapper objectMapper;

    @Value("${keycloak.target-realm}")
    private String targetRealm;

    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        List<RoleEntity> roles = roleRepository.findAll();
        return roles.stream()
                .map(role -> {
                    long count = userRepository.findWithFilters(null, role.getName(), null,
                            org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
                    return roleMapper.toResponse(role, count);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleDetailResponse getById(UUID id) {
        RoleEntity role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));

        List<UserResponse> assignedUsers = userRepository
                .findWithFilters(null, role.getName(), null, org.springframework.data.domain.Pageable.unpaged())
                .map(userMapper::toResponse)
                .getContent();

        return new RoleDetailResponse(
                role.getId(),
                role.getName(),
                role.getDisplayName(),
                role.getDescription(),
                parsePermissions(role.getPermissions()),
                assignedUsers.size(),
                role.isActive(),
                assignedUsers
        );
    }

    @Transactional
    public RoleResponse update(UUID id, RoleUpdateRequest request, String actorId) {
        RoleEntity role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));

        if (request.displayName() != null) {
            role.setDisplayName(request.displayName());
        }
        if (request.description() != null) {
            role.setDescription(request.description());
        }

        role = roleRepository.save(role);
        long userCount = userRepository.findWithFilters(null, role.getName(), null,
                org.springframework.data.domain.Pageable.unpaged()).getTotalElements();

        auditService.record("ROLE", id, actorId, "UPDATE", null, roleMapper.toResponse(role, userCount));
        return roleMapper.toResponse(role, userCount);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAssignedUsers(UUID roleId) {
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));

        return userRepository
                .findWithFilters(null, role.getName(), null, org.springframework.data.domain.Pageable.unpaged())
                .map(userMapper::toResponse)
                .getContent();
    }

    @Transactional
    public void assignUser(UUID roleId, UUID userId, String actorId) {
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (userRoleRepository.findByUserIdAndRoleId(userId, roleId).isPresent()) {
            throw new ConflictException("User '" + user.getUsername() + "' already assigned to role '" + role.getName() + "'");
        }

        assignKeycloakRole(userId.toString(), role.getName());

        UserRoleEntity userRole = UserRoleEntity.builder()
                .userId(userId)
                .roleId(roleId)
                .assignedBy(UUID.fromString(actorId))
                .build();
        userRoleRepository.save(userRole);

        user.setRole(role.getName());
        userRepository.save(user);

        auditService.record("ROLE", roleId, actorId, "ASSIGN_USER",
                null, java.util.Map.of("userId", userId, "role", role.getName()));
    }

    @Transactional
    public void unassignUser(UUID roleId, UUID userId, String actorId) {
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        UserRoleEntity userRole = userRoleRepository.findByUserIdAndRoleId(userId, roleId)
                .orElseThrow(() -> new BusinessRuleException(
                        "User '" + user.getUsername() + "' is not assigned to role '" + role.getName() + "'"));

        removeKeycloakRole(userId.toString(), role.getName());
        userRoleRepository.delete(userRole);

        auditService.record("ROLE", roleId, actorId, "UNASSIGN_USER",
                java.util.Map.of("userId", userId, "role", role.getName()), null);
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

    private RealmResource realmResource() {
        return keycloak.realm(targetRealm);
    }

    private void assignKeycloakRole(String keycloakUserId, String roleName) {
        RoleRepresentation role = realmResource().roles().get(roleName).toRepresentation();
        realmResource().users().get(keycloakUserId)
                .roles().realmLevel()
                .add(Collections.singletonList(role));
    }

    private void removeKeycloakRole(String keycloakUserId, String roleName) {
        RoleRepresentation role = realmResource().roles().get(roleName).toRepresentation();
        realmResource().users().get(keycloakUserId)
                .roles().realmLevel()
                .remove(Collections.singletonList(role));
    }
}
