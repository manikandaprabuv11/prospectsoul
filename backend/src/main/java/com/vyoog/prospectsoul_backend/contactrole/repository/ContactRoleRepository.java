package com.vyoog.prospectsoul_backend.contactrole.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.contactrole.entity.ContactRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRoleRepository extends JpaRepository<ContactRole, UUID> {

    Optional<ContactRole> findByKey(String key);

    List<ContactRole> findAllByOrderBySortOrderAscLabelAsc();

    List<ContactRole> findByActiveTrueOrderBySortOrderAscLabelAsc();
}
