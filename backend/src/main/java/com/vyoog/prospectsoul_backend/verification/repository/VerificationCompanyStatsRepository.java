package com.vyoog.prospectsoul_backend.verification.repository;

import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.company.entity.Company;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * The one aggregate read the Verify page needs over {@code companies} that is
 * not a filtered page.
 *
 * <p>Declared as a narrow {@link Repository} rather than added to
 * {@code CompanyRepository}: the Verify feature owns its own read model, and
 * this interface exposes nothing that could mutate a company.
 */
public interface VerificationCompanyStatsRepository extends Repository<Company, UUID> {

    /**
     * Distinct actors that have added companies, most prolific first, for the
     * Added By dropdown. Returns {@code [created_by, count]} rows.
     */
    @Query("""
            SELECT c.createdBy, COUNT(c) FROM Company c
            WHERE c.createdBy IS NOT NULL
            GROUP BY c.createdBy
            ORDER BY COUNT(c) DESC
            """)
    List<Object[]> countCompaniesByCreator();
}
