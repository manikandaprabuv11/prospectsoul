package com.vyoog.prospectsoul_backend.imports.normalization;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class CompanyNameNormalizer {

    private static final List<String> LEGAL_SUFFIXES = List.of(
            "pvt ltd", "pvt. ltd.", "pvt. ltd", "pvt ltd.",
            "private limited", "private ltd", "private ltd.",
            "limited", "ltd", "ltd.",
            "llp", "l.l.p.", "l.l.p",
            "inc", "inc.", "incorporated",
            "corp", "corp.", "corporation",
            "co", "co.", "company",
            "llc", "l.l.c.", "l.l.c",
            "plc", "p.l.c."
    );

    public String normalize(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String normalized = name.trim().toLowerCase();

        // strip punctuation except spaces
        normalized = normalized.replaceAll("[^a-z0-9\\s]", " ");

        // collapse whitespace
        normalized = normalized.replaceAll("\\s+", " ").trim();

        // strip legal suffixes (longest first to avoid partial matches)
        for (String suffix : LEGAL_SUFFIXES) {
            String normalizedSuffix = suffix.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
            if (normalized.endsWith(" " + normalizedSuffix)) {
                normalized = normalized.substring(0, normalized.length() - normalizedSuffix.length() - 1).trim();
                break;
            }
        }

        return normalized;
    }
}
