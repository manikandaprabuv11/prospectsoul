package com.vyoog.prospectsoul_backend.nic.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.nic.entity.NicCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NicCodeRepository extends JpaRepository<NicCode, UUID>,
        JpaSpecificationExecutor<NicCode> {

    Optional<NicCode> findByCode(String code);

    @Query("SELECT n FROM NicCode n WHERE n.parent.id = :parentId ORDER BY n.code ASC")
    List<NicCode> findByParentIdOrderByCodeAsc(@Param("parentId") UUID parentId);

    Page<NicCode> findByParentIsNullOrderByCodeAsc(Pageable pageable);

    // Docs 21 §4.2 recursive descendant walk. Kept in the repository so the
    // resulting UUID set can be reused by CompanyService in track C3.
    @Query(value = """
        WITH RECURSIVE subtree AS (
            SELECT id FROM nic_codes WHERE id = :rootId
            UNION ALL
            SELECT n.id
              FROM nic_codes n
              JOIN subtree s ON n.parent_id = s.id
        )
        SELECT id FROM subtree
        """, nativeQuery = true)
    List<UUID> findDescendantIds(@Param("rootId") UUID rootId);

    // Highest-code prefix already in the table, used by import to link a
    // child to the deepest parent that actually exists (real data skips
    // levels — Kickoff constraint 5).
    @Query(value = """
        SELECT id
          FROM nic_codes
         WHERE :childCode LIKE code || '%'
           AND code <> :childCode
         ORDER BY length(code) DESC
         LIMIT 1
        """, nativeQuery = true)
    Optional<UUID> findLongestPrefixParent(@Param("childCode") String childCode);

    List<NicCode> findByIsPrimaryTrueAndActiveTrueOrderByCodeAsc();

    @Query("SELECT COUNT(c) FROM NicCode c WHERE c.parent.id = :parentId")
    long countChildren(@Param("parentId") UUID parentId);

    @Query(value = """
        SELECT DISTINCT cnc.nic_code_id
          FROM company_nic_codes cnc
         WHERE cnc.nic_code_id = ANY(:ids)
        """, nativeQuery = true)
    Set<UUID> findReferencedNicCodeIds(@Param("ids") UUID[] ids);
}
