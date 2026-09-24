package com.vyoog.prospectsoul_backend.contact.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.contact.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactRepository extends JpaRepository<Contact, UUID> {

    List<Contact> findByCompanyIdOrderByIsPrimaryDescCreatedAtAsc(UUID companyId);

    List<Contact> findByCompanyId(UUID companyId);

    Optional<Contact> findFirstByCompanyIdAndIsPrimaryTrue(UUID companyId);

    long countByCompanyId(UUID companyId);

    @Modifying
    @Query("UPDATE Contact c SET c.isPrimary = false WHERE c.companyId = :companyId AND c.id <> :keepId")
    int demoteOtherPrimaries(@Param("companyId") UUID companyId, @Param("keepId") UUID keepId);

    /**
     * Primary contact per company for the given IDs; falls back to the
     * earliest non-primary if no row is flagged primary. Used to populate
     * the Companies list with 'Contact person' + 'Contact number'.
     */
    @Query("SELECT c FROM Contact c WHERE c.companyId IN :companyIds ORDER BY c.companyId ASC, c.isPrimary DESC, c.createdAt ASC")
    List<Contact> findFirstPerCompany(@Param("companyIds") List<UUID> companyIds);
}
