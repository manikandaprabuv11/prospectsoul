package com.vyoog.prospectsoul_backend.company.specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.company.entity.Company;
import com.vyoog.prospectsoul_backend.company.nic.entity.CompanyNicCode;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filter builder for the Companies list. The Sales-Intelligence extension
 * (docs 21 §4.2) adds NIC-tree filtering, pincode/district/region,
 * turnover / employee ranges, GST presence, and a contact-role facet.
 *
 * NIC descendant expansion is done in {@link com.vyoog.prospectsoul_backend.nic.repository.NicCodeRepository#findDescendantIds}
 * so the same recursive CTE serves the {@code view=grouped_by_nic} response.
 * The resolved id set is passed here as a plain collection, and the
 * membership is expressed as a correlated subquery over
 * {@code company_nic_codes} — this keeps the outer query paginable.
 */
public final class CompanySpecification {

    private CompanySpecification() {}

    public record Filters(
            String search, String city, String state,
            String industry, String cluster, String source,
            String pipelineState, String verificationStatus,
            // Sales-Intelligence facets
            String region, String district, String pincode,
            BigDecimal turnoverMin, BigDecimal turnoverMax,
            Integer employeeMin, Integer employeeMax,
            Boolean gstPresent,
            UUID nicCodeId,
            Collection<UUID> nicCodeIds,      // resolved descendant set — see repository
            UUID hasContactRoleId
    ) {}

    public static Specification<Company> withFilters(Filters f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (f.search != null && !f.search.isBlank()) {
                String pattern = "%" + f.search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("canonicalName")), pattern),
                        cb.like(cb.lower(root.get("normalizedName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("primaryPhoneNormalized")), pattern),
                        cb.like(cb.lower(root.get("websiteDomain")), pattern)
                ));
            }
            eqLower(f.city, root.get("city"), cb, predicates);
            eqLower(f.state, root.get("state"), cb, predicates);
            eqLower(f.industry, root.get("industry"), cb, predicates);
            eqLower(f.cluster, root.get("cluster"), cb, predicates);
            if (f.source != null && !f.source.isBlank()) predicates.add(cb.equal(root.get("source"), f.source));
            if (f.pipelineState != null && !f.pipelineState.isBlank())
                predicates.add(cb.equal(root.get("pipelineState"),
                        Company.PipelineState.valueOf(f.pipelineState)));
            if (f.verificationStatus != null && !f.verificationStatus.isBlank())
                predicates.add(cb.equal(root.get("verificationStatus"),
                        Company.VerificationStatus.valueOf(f.verificationStatus)));

            eqLower(f.region, root.get("region"), cb, predicates);
            eqLower(f.district, root.get("district"), cb, predicates);
            if (f.pincode != null && !f.pincode.isBlank())
                predicates.add(cb.equal(root.get("pincode"), f.pincode.trim()));

            if (f.turnoverMin != null) predicates.add(cb.greaterThanOrEqualTo(root.get("turnover"), f.turnoverMin));
            if (f.turnoverMax != null) predicates.add(cb.lessThanOrEqualTo(root.get("turnover"), f.turnoverMax));
            if (f.employeeMin != null) predicates.add(cb.greaterThanOrEqualTo(root.get("employeeCount"), f.employeeMin));
            if (f.employeeMax != null) predicates.add(cb.lessThanOrEqualTo(root.get("employeeCount"), f.employeeMax));
            if (Boolean.TRUE.equals(f.gstPresent))
                predicates.add(cb.and(cb.isNotNull(root.get("gstNumber")), cb.notEqual(root.get("gstNumber"), "")));
            if (Boolean.FALSE.equals(f.gstPresent))
                predicates.add(cb.or(cb.isNull(root.get("gstNumber")), cb.equal(root.get("gstNumber"), "")));

            // NIC filtering — either a single code, or a descendant set that
            // the caller already resolved via the recursive CTE (or an
            // intersection of the page selection with a configured default —
            // see CompanyController).
            //
            // A resolved-but-EMPTY set is not the same thing as "no NIC
            // filter was requested": the caller explicitly asked for a NIC
            // scope and it resolved to nothing (e.g. the configured default
            // NIC and the analyst's page-level pick don't overlap, or a
            // stale nic_parent_id points at a deleted code). That must still
            // yield zero companies. Previously this branch was gated on
            // `!nicSet.isEmpty()`, so an explicitly-empty set fell through
            // and the NIC filter was silently dropped — the query then
            // matched every company as if no NIC filter had been applied at
            // all, which is worse than an empty result and easy to mistake
            // for "pincode + NIC is broken" when it actually returns too
            // much rather than too little.
            Collection<UUID> nicSet = f.nicCodeIds;
            if (nicSet == null && f.nicCodeId != null) nicSet = List.of(f.nicCodeId);
            if (nicSet != null) {
                if (nicSet.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else if (query != null) {
                    Subquery<UUID> sub = query.subquery(UUID.class);
                    var cnc = sub.from(CompanyNicCode.class);
                    sub.select(cnc.get("companyId"))
                            .where(cnc.get("nicCode").get("id").in(nicSet));
                    predicates.add(root.get("id").in(sub));
                    query.distinct(true);
                }
            }

            if (f.hasContactRoleId != null && query != null) {
                Subquery<UUID> sub = query.subquery(UUID.class);
                var contact = sub.from(com.vyoog.prospectsoul_backend.contact.entity.Contact.class);
                sub.select(contact.get("companyId"))
                        .where(cb.equal(contact.get("role").get("id"), f.hasContactRoleId));
                predicates.add(root.get("id").in(sub));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void eqLower(String value, jakarta.persistence.criteria.Path<String> path,
                                 jakarta.persistence.criteria.CriteriaBuilder cb,
                                 List<Predicate> predicates) {
        if (value == null || value.isBlank()) return;
        predicates.add(cb.equal(cb.lower(path), value.toLowerCase()));
    }

    /**
     * Backward-compat shim for callers still on the flat 8-arg signature.
     * New code should use {@link #withFilters(Filters)}.
     */
    public static Specification<Company> withFilters(String search, String city, String state,
                                                      String industry, String cluster, String source,
                                                      String pipelineState, String verificationStatus) {
        return withFilters(new Filters(search, city, state, industry, cluster, source,
                pipelineState, verificationStatus,
                null, null, null, null, null, null, null, null, null, null, null));
    }

    private static void unused(JoinType j) { /* keep the import for future use */ }
}
