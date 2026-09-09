package com.vyoog.prospectsoul_backend.company.specification;

import java.util.ArrayList;
import java.util.List;

import com.vyoog.prospectsoul_backend.company.entity.Company;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public final class CompanySpecification {

    private CompanySpecification() {}

    public static Specification<Company> withFilters(String search, String city, String state,
                                                      String industry, String cluster, String source,
                                                      String pipelineState, String verificationStatus) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("canonicalName")), pattern),
                        cb.like(cb.lower(root.get("normalizedName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("primaryPhoneNormalized")), pattern),
                        cb.like(cb.lower(root.get("websiteDomain")), pattern)
                ));
            }
            if (city != null && !city.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("city")), city.toLowerCase()));
            }
            if (state != null && !state.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("state")), state.toLowerCase()));
            }
            if (industry != null && !industry.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("industry")), industry.toLowerCase()));
            }
            if (cluster != null && !cluster.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("cluster")), cluster.toLowerCase()));
            }
            if (source != null && !source.isBlank()) {
                predicates.add(cb.equal(root.get("source"), source));
            }
            if (pipelineState != null && !pipelineState.isBlank()) {
                predicates.add(cb.equal(root.get("pipelineState"),
                        Company.PipelineState.valueOf(pipelineState)));
            }
            if (verificationStatus != null && !verificationStatus.isBlank()) {
                predicates.add(cb.equal(root.get("verificationStatus"),
                        Company.VerificationStatus.valueOf(verificationStatus)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
