package com.vyoog.prospectsoul_backend.imports.normalization;

import org.springframework.stereotype.Component;

@Component
public class WebsiteNormalizer {

    public String normalize(String website) {
        if (website == null || website.isBlank()) {
            return null;
        }
        String domain = website.trim().toLowerCase();

        // strip scheme
        if (domain.startsWith("https://")) {
            domain = domain.substring(8);
        } else if (domain.startsWith("http://")) {
            domain = domain.substring(7);
        }

        // strip www.
        if (domain.startsWith("www.")) {
            domain = domain.substring(4);
        }

        // strip path/query/fragment
        int pathIdx = domain.indexOf('/');
        if (pathIdx > 0) {
            domain = domain.substring(0, pathIdx);
        }
        int queryIdx = domain.indexOf('?');
        if (queryIdx > 0) {
            domain = domain.substring(0, queryIdx);
        }
        int fragmentIdx = domain.indexOf('#');
        if (fragmentIdx > 0) {
            domain = domain.substring(0, fragmentIdx);
        }

        // strip trailing dot
        if (domain.endsWith(".")) {
            domain = domain.substring(0, domain.length() - 1);
        }

        return domain.isBlank() ? null : domain;
    }
}
