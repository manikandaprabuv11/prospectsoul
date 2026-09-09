package com.vyoog.prospectsoul_backend.admin.users.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.admin.users.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UUID> {

    List<UserRoleEntity> findByUserId(UUID userId);

    Optional<UserRoleEntity> findByUserIdAndRoleId(UUID userId, UUID roleId);

    void deleteByUserId(UUID userId);
}
