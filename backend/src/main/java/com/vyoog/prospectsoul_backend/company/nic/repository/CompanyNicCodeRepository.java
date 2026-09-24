package com.vyoog.prospectsoul_backend.company.nic.repository;

import java.util.Collection;
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

    /**
     * Company ids that have any of the given NIC codes attached (primary or
     * secondary) — same semantics as {@link com.vyoog.prospectsoul_backend.company.specification.CompanySpecification}'s
     * NIC subquery, reused by the Companies Map endpoint so the NIC filter
     * behaves identically on both screens.
     */
    @Query("select distinct c.companyId from CompanyNicCode c where c.nicCode.id in :nicCodeIds")
    List<UUID> findCompanyIdsByNicCodeIdIn(@Param("nicCodeIds") Collection<UUID> nicCodeIds);

    Optional<CompanyNicCode> findFirstByCompanyIdAndIsPrimaryTrue(UUID companyId);

    Optional<CompanyNicCode> findByCompanyIdAndNicCodeRaw(UUID companyId, String raw);

    @Modifying
    @Query("UPDATE CompanyNicCode c SET c.isPrimary = false WHERE c.companyId = :companyId AND c.id <> :keepId")
    int demoteOthers(@Param("companyId") UUID companyId, @Param("keepId") UUID keepId);

    long countByCompanyId(UUID companyId);

    /** Bulk fetch NIC join rows for a page of companies. */
    List<CompanyNicCode> findByCompanyIdInOrderByCompanyIdAscSequenceNoAsc(java.util.List<UUID> companyIds);
}
