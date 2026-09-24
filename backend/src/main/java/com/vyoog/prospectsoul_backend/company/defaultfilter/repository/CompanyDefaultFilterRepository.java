package com.vyoog.prospectsoul_backend.company.defaultfilter.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.company.defaultfilter.entity.CompanyDefaultFilter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyDefaultFilterRepository extends JpaRepository<CompanyDefaultFilter, UUID> {
    List<CompanyDefaultFilter> findAllByOrderBySortOrderAscLabelAsc();
    List<CompanyDefaultFilter> findByActiveTrueOrderBySortOrderAscLabelAsc();
    Optional<CompanyDefaultFilter> findByFilterKey(String filterKey);
}
