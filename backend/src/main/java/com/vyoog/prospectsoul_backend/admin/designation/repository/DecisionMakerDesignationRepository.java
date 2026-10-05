package com.vyoog.prospectsoul_backend.admin.designation.repository;

import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.admin.designation.entity.DecisionMakerDesignation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionMakerDesignationRepository extends JpaRepository<DecisionMakerDesignation, UUID> {

    List<DecisionMakerDesignation> findByActiveTrue();

    boolean existsByDesignationIgnoreCase(String designation);
}
