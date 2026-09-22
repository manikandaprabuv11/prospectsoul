package com.vyoog.prospectsoul_backend.company.nic.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.company.nic.entity.CompanyNicCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyNicCodeRepository extends JpaRepository<CompanyNicCode, UUID> {

    List<CompanyNicCode> findByCompanyIdOrderBySequenceNoAsc(UUID companyId);

    Optional<CompanyNicCode> findFirstByCompanyIdAndIsPrimaryTrue(UUID companyId);

    Optional<CompanyNicCode> findByCompanyIdAndNicCodeRaw(UUID companyId, String raw);

    @Modifying
    @Query("UPDATE CompanyNicCode c SET c.isPrimary = false WHERE c.companyId = :companyId AND c.id <> :keepId")
    int demoteOthers(@Param("companyId") UUID companyId, @Param("keepId") UUID keepId);

    long countByCompanyId(UUID companyId);
}
