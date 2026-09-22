package com.vyoog.prospectsoul_backend.company.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Response shape for {@code GET /companies?view=grouped_by_nic&nic_parent_id=...}
 * per docs 21 §4.3. Companies are lazy-loaded per group via
 * {@code GET /companies?nic_code_id=<uuid>}.
 */
public record CompanyGroupedByNicResponse(
        String view,
        NodeRef rootNode,
        List<Group> groups,
        long totalCompanies
) {
    public record NodeRef(UUID id, String code, String description) {}

    /**
     * One row per direct child of the root, plus a synthetic "Directly
     * tagged to root" bucket (node == null) — the latter surfaces companies
     * classified at the parent level itself with no sub-code.
     */
    public record Group(NodeRef node, String label, long count) {}
}
