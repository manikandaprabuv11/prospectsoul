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

    Optional<Contact> findFirstByCompanyIdAndIsPrimaryTrue(UUID companyId);

    long countByCompanyId(UUID companyId);

    @Modifying
    @Query("UPDATE Contact c SET c.isPrimary = false WHERE c.companyId = :companyId AND c.id <> :keepId")
    int demoteOtherPrimaries(@Param("companyId") UUID companyId, @Param("keepId") UUID keepId);
}
