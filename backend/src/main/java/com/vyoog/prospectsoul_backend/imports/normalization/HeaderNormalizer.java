package com.vyoog.prospectsoul_backend.imports.normalization;

import org.springframework.stereotype.Component;

@Component
public class HeaderNormalizer {

    public String normalize(String header) {
        if (header == null || header.isBlank()) {
            return "";
        }

        // split camelCase/PascalCase: "CompanyName" -> "Company Name"
        String expanded = header.replaceAll("([a-z])([A-Z])", "$1 $2");

        // lowercase
        String lower = expanded.toLowerCase();

        // replace underscores, hyphens, dots, and other punctuation with spaces
        String cleaned = lower.replaceAll("[_\\-./,;:()\\[\\]{}]", " ");

        // strip remaining non-alphanumeric non-space characters
        cleaned = cleaned.replaceAll("[^a-z0-9\\s]", " ");

        // collapse whitespace and trim
        return cleaned.replaceAll("\\s+", " ").trim();
    }
}
