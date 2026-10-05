package com.vyoog.prospectsoul_backend.company.phone.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.company.phone.entity.CompanyPhone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CompanyPhoneRepository extends JpaRepository<CompanyPhone, UUID> {

    List<CompanyPhone> findByCompanyIdOrderByIsPrimaryDescConfidenceAscCreatedAtDesc(UUID companyId);

    Optional<CompanyPhone> findByCompanyIdAndIsPrimaryTrue(UUID companyId);

    @Query("SELECT cp FROM CompanyPhone cp WHERE cp.numberNormalized = :normalized AND cp.numberNormalized IS NOT NULL")
    List<CompanyPhone> findByNumberNormalized(String normalized);

    @Query("SELECT cp FROM CompanyPhone cp WHERE cp.numberNormalized = :normalized AND cp.company.id = :companyId")
    Optional<CompanyPhone> findByCompanyIdAndNumberNormalized(UUID companyId, String normalized);

    @Query("SELECT cp FROM CompanyPhone cp WHERE cp.numberNormalized IN :numbers AND cp.numberNormalized IS NOT NULL")
    List<CompanyPhone> findByNumberNormalizedIn(List<String> numbers);

    List<CompanyPhone> findByContactId(UUID contactId);

    int countByCompanyId(UUID companyId);

    @Modifying
    @Query("UPDATE CompanyPhone cp SET cp.isPrimary = false WHERE cp.company.id = :companyId AND cp.isPrimary = true AND cp.id != :excludeId")
    void clearOtherPrimaries(UUID companyId, UUID excludeId);

    @Modifying
    @Query("UPDATE CompanyPhone cp SET cp.isPrimary = false WHERE cp.company.id = :companyId AND cp.isPrimary = true")
    void clearAllPrimaries(UUID companyId);

    void deleteByCompanyId(UUID companyId);
}
