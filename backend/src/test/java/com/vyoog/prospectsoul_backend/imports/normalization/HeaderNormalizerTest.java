package com.vyoog.prospectsoul_backend.imports.normalization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeaderNormalizerTest {

    private final HeaderNormalizer normalizer = new HeaderNormalizer();

    @ParameterizedTest
    @CsvSource({
            "'Company Name',    'company name'",
            "'company_name',    'company name'",
            "'COMPANY-NAME',    'company name'",
            "'CompanyName',     'company name'",
            "'  Company  Name ',  'company name'",
            "'Phone No.',       'phone no'",
            "'E-mail ID',       'e mail id'",
            "'Website URL',     'website url'",
            "'BusinessType',    'business type'",
    })
    void normalizesVariousFormats(String input, String expected) {
        assertEquals(expected, normalizer.normalize(input));
    }

    @Test
    void normalizesNullAndBlank() {
        assertEquals("", normalizer.normalize(null));
        assertEquals("", normalizer.normalize(""));
        assertEquals("", normalizer.normalize("   "));
    }
}
