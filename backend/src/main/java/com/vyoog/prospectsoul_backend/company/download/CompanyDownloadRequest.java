package com.vyoog.prospectsoul_backend.company.download;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Pattern;

/**
 * POST body for {@code /api/v1/companies/download}. Deliberately mirrors
 * the flat list filter parameters so an analyst can download exactly what
 * they see on the Companies List — ADR-0005 keeps this endpoint distinct
 * from the pipeline Export.
 */
public record CompanyDownloadRequest(
        Filter filter,
        @Pattern(regexp = "^(csv|xlsx)$", message = "format must be 'csv' or 'xlsx'")
        String format,
        List<String> columns,
        Boolean includeAllContacts
) {
    public record Filter(
            String q,
            String city,
            String state,
            String industry,
            String cluster,
            String source,
            String pipelineState,
            String verificationStatus,
            String region,
            String district,
            String pincode,
            BigDecimal turnoverMin,
            BigDecimal turnoverMax,
            Integer employeeMin,
            Integer employeeMax,
            Boolean gstPresent,
            UUID nicCodeId,
            UUID nicParentId,
            Boolean nicIncludeDescendants,
            UUID hasContactRoleId
    ) {}
}
