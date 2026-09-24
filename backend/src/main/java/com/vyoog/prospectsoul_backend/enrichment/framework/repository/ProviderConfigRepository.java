package com.vyoog.prospectsoul_backend.enrichment.framework.repository;

import com.vyoog.prospectsoul_backend.enrichment.framework.entity.ProviderConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderConfigRepository extends JpaRepository<ProviderConfigEntity, String> {
}
