package com.vyoog.prospectsoul_backend.verification.specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.verification.entity.VerificationBatch;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationBatchItem;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationBatchStatus;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationItemStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import com.vyoog.prospectsoul_backend.company.entity.Company;

/** Filters for the history table and the job-detail item list. */
public final class VerificationBatchSpecification {

    private VerificationBatchSpecification() {}

    /** Previous verification jobs, filtered by status, requester and date. */
    public static Specification<VerificationBatch> withFilters(VerificationBatchStatus status,
                                                                String requestedBy,
                                                                Instant from,
                                                                Instant toExclusive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (requestedBy != null) {
                predicates.add(cb.equal(root.get("requestedBy"), requestedBy));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (toExclusive != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), toExclusive));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Item results for one batch.
     *
     * <p>{@code search} has to match the company name, which lives on another
     * table, so it is expressed as an EXISTS subquery over {@code companies}.
     * That keeps the filter in SQL — the alternative, loading the page and
     * filtering names in Java, would break pagination.
     */
    public static Specification<VerificationBatchItem> itemsOf(UUID batchId,
                                                                VerificationItemStatus status,
                                                                String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("batchId"), batchId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (search != null && query != null) {
                Subquery<UUID> matchingCompanies = query.subquery(UUID.class);
                Root<Company> company = matchingCompanies.from(Company.class);
                matchingCompanies.select(company.get("id"))
                        .where(cb.and(
                                cb.equal(company.get("id"), root.get("companyId")),
                                cb.like(cb.lower(company.get("canonicalName")), search)));

                predicates.add(cb.or(
                        cb.exists(matchingCompanies),
                        cb.like(cb.lower(cb.coalesce(root.get("phoneNumber"), "")), search),
                        cb.like(cb.lower(cb.coalesce(root.get("normalizedPhoneNumber"), "")), search)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
