package com.vyoog.prospectsoul_backend.imports.normalization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompanyNameNormalizerTest {

    private final CompanyNameNormalizer normalizer = new CompanyNameNormalizer();

    @ParameterizedTest
    @CsvSource({
            "'Acme Corp Pvt. Ltd.',     'acme corp'",
            "'ACME CORP PVT LTD',       'acme corp'",
            "'Acme Corp Private Limited', 'acme corp'",
            "'  Acme   Industries  ',     'acme industries'",
            "'Acme Corp Ltd.',           'acme corp'",
            "'Acme Corp LLP',            'acme corp'",
    })
    void normalizesAndStripsLegalSuffixes(String input, String expected) {
        assertEquals(expected, normalizer.normalize(input));
    }

    @Test
    void handlesNullAndBlank() {
        assertEquals("", normalizer.normalize(null));
        assertEquals("", normalizer.normalize(""));
        assertEquals("", normalizer.normalize("   "));
    }

    @Test
    void preservesNameWithoutSuffix() {
        assertEquals("tech solutions", normalizer.normalize("Tech Solutions"));
    }
}
