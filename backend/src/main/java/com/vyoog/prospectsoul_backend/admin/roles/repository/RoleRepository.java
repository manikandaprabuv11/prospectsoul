package com.vyoog.prospectsoul_backend.admin.roles.repository;

import java.util.Optional;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.admin.roles.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByName(String name);
}
