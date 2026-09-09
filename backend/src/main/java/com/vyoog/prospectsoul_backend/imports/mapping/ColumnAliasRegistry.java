package com.vyoog.prospectsoul_backend.imports.mapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vyoog.prospectsoul_backend.imports.normalization.HeaderNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ColumnAliasRegistry {

    private final HeaderNormalizer headerNormalizer;

    private static final Map<String, List<String>> ALIAS_MAP = new LinkedHashMap<>();

    static {
        ALIAS_MAP.put("canonical_name", List.of(
                "Company", "Company Name", "Company_Name", "CompanyName",
                "Organization", "Organization Name", "Organisation",
                "Firm", "Firm Name", "Business Name", "Legal Name"
        ));
        ALIAS_MAP.put("primary_phone_normalized", List.of(
                "Phone", "Phone Number", "Phone No", "Phone No.",
                "Mobile", "Mobile Number", "Mobile No",
                "Contact Number", "Contact No",
                "Telephone", "Telephone Number",
                "Business Phone", "Office Phone", "Contact Phone"
        ));
        ALIAS_MAP.put("email", List.of(
                "Email", "Email ID", "Email Address",
                "E-mail", "E-mail ID", "Mail",
                "Company Email", "Business Email"
        ));
        ALIAS_MAP.put("website_domain", List.of(
                "Website", "Website URL", "Web", "URL",
                "Company Website", "Web Address", "Domain"
        ));
        ALIAS_MAP.put("city", List.of(
                "City", "Town", "Location", "Company City", "Business Location"
        ));
        ALIAS_MAP.put("state", List.of(
                "State", "State Name", "Province", "Region", "Company State"
        ));
        ALIAS_MAP.put("industry", List.of(
                "Industry", "Business Type", "Sector", "Business Sector", "Industry Type"
        ));
        ALIAS_MAP.put("cluster", List.of(
                "Cluster", "Industrial Cluster", "Business Cluster"
        ));
        ALIAS_MAP.put("size_band", List.of(
                "Size Band", "Size", "Company Size", "Employee Range"
        ));
        ALIAS_MAP.put("tags", List.of(
                "Tags", "Labels", "Keywords"
        ));
        ALIAS_MAP.put("source", List.of(
                "Source", "Data Source", "Lead Source"
        ));
    }

    public Set<String> getTargetFields() {
        return ALIAS_MAP.keySet();
    }

    public List<String> getAliases(String targetField) {
        return ALIAS_MAP.getOrDefault(targetField, List.of());
    }

    public record MatchResult(String targetField, String matchType, double confidence) {}

    public MatchResult match(String sourceHeader) {
        String normalizedSource = headerNormalizer.normalize(sourceHeader);

        // 1. exact normalized alias match
        for (var entry : ALIAS_MAP.entrySet()) {
            for (String alias : entry.getValue()) {
                String normalizedAlias = headerNormalizer.normalize(alias);
                if (normalizedAlias.equals(normalizedSource)) {
                    return new MatchResult(entry.getKey(), "EXACT_ALIAS", 1.0);
                }
            }
        }

        // 2. target field name match (e.g. "canonical_name" -> "canonical name")
        for (String targetField : ALIAS_MAP.keySet()) {
            String normalizedTarget = headerNormalizer.normalize(targetField);
            if (normalizedTarget.equals(normalizedSource)) {
                return new MatchResult(targetField, "FIELD_NAME", 1.0);
            }
        }

        // 3. fuzzy containment: if the source contains a known alias word pattern
        for (var entry : ALIAS_MAP.entrySet()) {
            for (String alias : entry.getValue()) {
                String normalizedAlias = headerNormalizer.normalize(alias);
                if (normalizedSource.contains(normalizedAlias) || normalizedAlias.contains(normalizedSource)) {
                    if (normalizedSource.length() >= 3 && normalizedAlias.length() >= 3) {
                        return new MatchResult(entry.getKey(), "FUZZY", 0.6);
                    }
                }
            }
        }

        return null;
    }

    public Map<String, List<String>> getAllAliases() {
        return new LinkedHashMap<>(ALIAS_MAP);
    }
}
