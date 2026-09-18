package com.vyoog.prospectsoul_backend.verification.specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.vyoog.prospectsoul_backend.company.entity.Company;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 * The verification module's company filters, following the existing
 * {@code CompanySpecification} pattern.
 *
 * <p>Specifications rather than a JPQL query with {@code :param IS NULL OR …}
 * clauses: that pattern makes PostgreSQL bind a parameter whose only context
 * is {@code IS NULL}, which it rejects with "could not determine data type of
 * parameter". Building the predicate list only from the filters that are
 * actually present avoids the untyped bind entirely, and keeps every filter in
 * SQL rather than in Java.
 */
public final class VerificationCompanySpecification {

    private VerificationCompanySpecification() {}

    /**
     * Selection candidates for "Verify New Companies".
     *
     * <p>Eligibility is "not currently VERIFIED", which admits both
     * {@code UNVERIFIED} and {@code INVALIDATED} — a company whose core field
     * was edited after verification is genuinely unverified again and must be
     * re-verifiable. A non-null {@code verificationStatus} narrows it to
     * exactly one of those.
     *
     * @param createdFrom       inclusive lower bound on {@code created_at}
     * @param createdToExclusive exclusive upper bound, so an inclusive
     *                           {@code date_to} of the 9th still includes the 9th
     * @param search            lower-cased term matched against name and phone
     */
    public static Specification<Company> eligible(Company.VerificationStatus verificationStatus,
                                                   String addedBy,
                                                   Instant createdFrom,
                                                   Instant createdToExclusive,
                                                   String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.notEqual(root.get("verificationStatus"),
                    Company.VerificationStatus.VERIFIED));
            predicates.add(cb.notEqual(root.get("pipelineState"),
                    Company.PipelineState.ARCHIVED));

            if (verificationStatus != null) {
                predicates.add(cb.equal(root.get("verificationStatus"), verificationStatus));
            }
            if (addedBy != null) {
                predicates.add(cb.equal(root.get("createdBy"), addedBy));
            }
            if (createdFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdToExclusive != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), createdToExclusive));
            }
            if (search != null) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("canonicalName")), search),
                        cb.like(cb.lower(cb.coalesce(root.get("primaryPhoneNormalized"), "")), search)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * The Verify page's bottom table. The {@code = VERIFIED} predicate is the
     * module's core invariant and is always applied, so no caller can widen it.
     */
    public static Specification<Company> verified(String search, String verifiedBy,
                                                   Instant verifiedFrom, Instant verifiedToExclusive,
                                                   String addedBy) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("verificationStatus"),
                    Company.VerificationStatus.VERIFIED));

            if (verifiedBy != null) {
                predicates.add(cb.equal(root.get("verifiedBy"), verifiedBy));
            }
            if (addedBy != null) {
                predicates.add(cb.equal(root.get("createdBy"), addedBy));
            }
            if (verifiedFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("verifiedAt"), verifiedFrom));
            }
            if (verifiedToExclusive != null) {
                predicates.add(cb.lessThan(root.get("verifiedAt"), verifiedToExclusive));
            }
            if (search != null) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("canonicalName")), search),
                        cb.like(cb.lower(cb.coalesce(root.get("primaryPhoneNormalized"), "")), search),
                        cb.like(cb.lower(cb.coalesce(root.get("city"), "")), search),
                        cb.like(cb.lower(cb.coalesce(root.get("email"), "")), search)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
