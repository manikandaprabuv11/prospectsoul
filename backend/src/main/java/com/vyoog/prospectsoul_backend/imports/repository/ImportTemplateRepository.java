package com.vyoog.prospectsoul_backend.imports.repository;

import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.imports.entity.ImportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportTemplateRepository extends JpaRepository<ImportTemplate, UUID> {

    List<ImportTemplate> findAllByOrderByCreatedAtDesc();
}
