package com.vyoog.prospectsoul_backend.admin.users.repository;

import java.util.Optional;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.admin.users.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("""
            SELECT u FROM UserEntity u
            WHERE (:search IS NULL
                   OR LOWER(u.fullName) LIKE :search
                   OR LOWER(u.username) LIKE :search
                   OR LOWER(u.email) LIKE :search)
            AND (:role IS NULL OR u.role = :role)
            AND (:active IS NULL OR u.active = :active)
            """)
    Page<UserEntity> findWithFilters(@Param("search") String search,
                                      @Param("role") String role,
                                      @Param("active") Boolean active,
                                      Pageable pageable);
}
