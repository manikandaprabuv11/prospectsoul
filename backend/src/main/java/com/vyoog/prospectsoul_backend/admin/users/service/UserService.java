package com.vyoog.prospectsoul_backend.admin.users.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.admin.users.dto.request.UserCreateRequest;
import com.vyoog.prospectsoul_backend.admin.users.dto.request.UserUpdateRequest;
import com.vyoog.prospectsoul_backend.admin.users.dto.response.UserResponse;
import com.vyoog.prospectsoul_backend.admin.users.entity.UserEntity;
import com.vyoog.prospectsoul_backend.admin.users.entity.UserRoleEntity;
import com.vyoog.prospectsoul_backend.admin.users.mapper.UserMapper;
import com.vyoog.prospectsoul_backend.admin.users.repository.UserRepository;
import com.vyoog.prospectsoul_backend.admin.users.repository.UserRoleRepository;
import com.vyoog.prospectsoul_backend.common.audit.service.AuditService;
import com.vyoog.prospectsoul_backend.common.dto.PageResponse;
import com.vyoog.prospectsoul_backend.common.exception.BusinessRuleException;
import com.vyoog.prospectsoul_backend.common.exception.ConflictException;
import com.vyoog.prospectsoul_backend.common.exception.ResourceNotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> VALID_ROLES = Set.of(
            "PS_ANALYST", "PS_SALES_LEAD", "PS_ADMIN", "PS_VIEWER", "PS_COO"
    );
    private static final Set<String> ALLOWED_SORT_FIELDS = new HashSet<>(Arrays.asList(
            "fullName", "username", "email", "role", "active", "createdAt", "lastLoginAt"
    ));

    private static final java.util.Map<String, UUID> ROLE_ID_MAP = java.util.Map.of(
            "PS_ANALYST", UUID.fromString("10000000-0000-0000-0000-000000000001"),
            "PS_SALES_LEAD", UUID.fromString("10000000-0000-0000-0000-000000000002"),
            "PS_ADMIN", UUID.fromString("10000000-0000-0000-0000-000000000003"),
            "PS_VIEWER", UUID.fromString("10000000-0000-0000-0000-000000000004"),
            "PS_COO", UUID.fromString("10000000-0000-0000-0000-000000000005")
    );

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserMapper userMapper;
    private final AuditService auditService;
    private final Keycloak keycloak;

    @Value("${keycloak.target-realm}")
    private String targetRealm;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(String search, String role, Boolean active,
                                            int page, int size, String sortField, String sortDir) {
        size = Math.min(size, MAX_PAGE_SIZE);
        String resolvedSort = ALLOWED_SORT_FIELDS.contains(sortField) ? sortField : "fullName";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, resolvedSort));

        String searchPattern = (search != null && !search.isBlank())
                ? "%" + search.toLowerCase() + "%" : null;
        Page<UserEntity> result = userRepository.findWithFilters(searchPattern, role, active, pageable);
        return PageResponse.from(result.map(userMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse create(UserCreateRequest request, String actorId) {
        validateRole(request.role());

        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username '" + request.username() + "' already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email '" + request.email() + "' already exists");
        }

        String keycloakUserId = createKeycloakUser(request);
        UUID userId = UUID.fromString(keycloakUserId);

        try {
            assignKeycloakRole(keycloakUserId, request.role());
        } catch (Exception e) {
            deleteKeycloakUser(keycloakUserId);
            throw new BusinessRuleException("Failed to assign role in Keycloak: " + e.getMessage());
        }

        UserEntity user = UserEntity.builder()
                .id(userId)
                .username(request.username())
                .fullName(request.fullName())
                .email(request.email())
                .role(request.role())
                .active(true)
                .build();

        user = userRepository.save(user);

        UUID roleId = ROLE_ID_MAP.get(request.role());
        if (roleId != null) {
            UserRoleEntity userRole = UserRoleEntity.builder()
                    .userId(userId)
                    .roleId(roleId)
                    .assignedBy(UUID.fromString(actorId))
                    .build();
            userRoleRepository.save(userRole);
        }

        auditService.record("USER", userId, actorId, "CREATE", null, userMapper.toResponse(user));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest request, String actorId) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        UserResponse previousState = userMapper.toResponse(user);

        boolean keycloakUpdateNeeded = false;

        if (request.fullName() != null) {
            user.setFullName(request.fullName());
            keycloakUpdateNeeded = true;
        }
        if (request.email() != null) {
            if (!request.email().equals(user.getEmail()) && userRepository.existsByEmail(request.email())) {
                throw new ConflictException("Email '" + request.email() + "' already exists");
            }
            user.setEmail(request.email());
            keycloakUpdateNeeded = true;
        }

        if (request.role() != null && !request.role().equals(user.getRole())) {
            validateRole(request.role());
            String oldRole = user.getRole();
            changeKeycloakRole(id.toString(), oldRole, request.role());

            userRoleRepository.deleteByUserId(id);
            UUID newRoleId = ROLE_ID_MAP.get(request.role());
            if (newRoleId != null) {
                UserRoleEntity userRole = UserRoleEntity.builder()
                        .userId(id)
                        .roleId(newRoleId)
                        .assignedBy(UUID.fromString(actorId))
                        .build();
                userRoleRepository.save(userRole);
            }
            user.setRole(request.role());
        } else if (keycloakUpdateNeeded) {
            updateKeycloakUser(id.toString(), user.getFullName(), user.getEmail());
        }

        user = userRepository.save(user);
        auditService.record("USER", id, actorId, "UPDATE", previousState, userMapper.toResponse(user));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse activate(UUID id, String actorId) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (user.isActive()) {
            throw new BusinessRuleException("User is already active");
        }

        enableKeycloakUser(id.toString(), true);
        user.setActive(true);
        user = userRepository.save(user);

        auditService.record("USER", id, actorId, "ACTIVATE", null, userMapper.toResponse(user));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse deactivate(UUID id, String actorId) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (!user.isActive()) {
            throw new BusinessRuleException("User is already inactive");
        }

        enableKeycloakUser(id.toString(), false);
        user.setActive(false);
        user = userRepository.save(user);

        auditService.record("USER", id, actorId, "DEACTIVATE", null, userMapper.toResponse(user));
        return userMapper.toResponse(user);
    }

    private void validateRole(String role) {
        if (!VALID_ROLES.contains(role)) {
            throw new BusinessRuleException("Invalid role: " + role
                    + ". Valid roles: " + VALID_ROLES);
        }
    }

    private RealmResource realmResource() {
        return keycloak.realm(targetRealm);
    }

    private String createKeycloakUser(UserCreateRequest request) {
        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setUsername(request.username());
        kcUser.setEmail(request.email());
        kcUser.setEnabled(true);
        kcUser.setEmailVerified(true);

        String[] nameParts = request.fullName().split(" ", 2);
        kcUser.setFirstName(nameParts[0]);
        if (nameParts.length > 1) {
            kcUser.setLastName(nameParts[1]);
        }

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.password());
        credential.setTemporary(true);
        kcUser.setCredentials(Collections.singletonList(credential));

        UsersResource usersResource = realmResource().users();
        try (Response response = usersResource.create(kcUser)) {
            if (response.getStatus() == 201) {
                String locationHeader = response.getHeaderString("Location");
                return locationHeader.substring(locationHeader.lastIndexOf('/') + 1);
            } else if (response.getStatus() == 409) {
                throw new ConflictException("User already exists in Keycloak");
            } else {
                throw new BusinessRuleException("Keycloak user creation failed with status "
                        + response.getStatus());
            }
        }
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

    private void changeKeycloakRole(String keycloakUserId, String oldRole, String newRole) {
        removeKeycloakRole(keycloakUserId, oldRole);
        assignKeycloakRole(keycloakUserId, newRole);
        updateKeycloakUser(keycloakUserId, null, null);
    }

    private void updateKeycloakUser(String keycloakUserId, String fullName, String email) {
        UserResource userResource = realmResource().users().get(keycloakUserId);
        UserRepresentation kcUser = userResource.toRepresentation();

        if (fullName != null) {
            String[] nameParts = fullName.split(" ", 2);
            kcUser.setFirstName(nameParts[0]);
            kcUser.setLastName(nameParts.length > 1 ? nameParts[1] : "");
        }
        if (email != null) {
            kcUser.setEmail(email);
        }

        userResource.update(kcUser);
    }

    private void enableKeycloakUser(String keycloakUserId, boolean enabled) {
        UserResource userResource = realmResource().users().get(keycloakUserId);
        UserRepresentation kcUser = userResource.toRepresentation();
        kcUser.setEnabled(enabled);
        userResource.update(kcUser);
    }

    private void deleteKeycloakUser(String keycloakUserId) {
        try {
            realmResource().users().get(keycloakUserId).remove();
        } catch (Exception e) {
            log.warn("Failed to delete Keycloak user {} during rollback: {}", keycloakUserId, e.getMessage());
        }
    }
}
