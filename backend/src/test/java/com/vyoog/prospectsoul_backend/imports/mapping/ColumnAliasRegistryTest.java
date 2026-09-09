package com.vyoog.prospectsoul_backend.imports.mapping;

import com.vyoog.prospectsoul_backend.imports.normalization.HeaderNormalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ColumnAliasRegistryTest {

    private final ColumnAliasRegistry registry = new ColumnAliasRegistry(new HeaderNormalizer());

    @ParameterizedTest
    @CsvSource({
            "'Company Name',        'canonical_name'",
            "'Company_Name',        'canonical_name'",
            "'CompanyName',         'canonical_name'",
            "'Organization',        'canonical_name'",
            "'Firm Name',           'canonical_name'",
            "'Business Name',       'canonical_name'",
            "'Legal Name',          'canonical_name'",
            "'Phone',               'primary_phone_normalized'",
            "'Phone Number',        'primary_phone_normalized'",
            "'Mobile No',           'primary_phone_normalized'",
            "'Telephone Number',    'primary_phone_normalized'",
            "'Contact Number',      'primary_phone_normalized'",
            "'Business Phone',      'primary_phone_normalized'",
            "'Email',               'email'",
            "'Email Address',       'email'",
            "'E-mail ID',           'email'",
            "'Company Email',       'email'",
            "'Website',             'website_domain'",
            "'Website URL',         'website_domain'",
            "'Web Address',         'website_domain'",
            "'City',                'city'",
            "'Town',                'city'",
            "'Location',            'city'",
            "'State',               'state'",
            "'Province',            'state'",
            "'Region',              'state'",
            "'Industry',            'industry'",
            "'Business Type',       'industry'",
            "'Sector',              'industry'",
    })
    void matchesKnownAliases(String header, String expectedField) {
        ColumnAliasRegistry.MatchResult result = registry.match(header);
        assertNotNull(result, "Should match: " + header);
        assertEquals(expectedField, result.targetField());
        assertEquals(1.0, result.confidence());
    }

    @Test
    void returnsNullForUnknownHeaders() {
        assertNull(registry.match("zzz_unknown_field"));
    }

    @Test
    void matchesCaseInsensitively() {
        ColumnAliasRegistry.MatchResult result = registry.match("COMPANY NAME");
        assertNotNull(result);
        assertEquals("canonical_name", result.targetField());
    }
}
