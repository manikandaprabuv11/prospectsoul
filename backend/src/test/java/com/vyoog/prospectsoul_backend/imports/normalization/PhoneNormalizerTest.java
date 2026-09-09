package com.vyoog.prospectsoul_backend.imports.normalization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoneNormalizerTest {

    private final PhoneNormalizer normalizer = new PhoneNormalizer();

    @ParameterizedTest
    @CsvSource({
            "'+91 98765 43210',   '9876543210'",
            "'0091-9876543210',   '9876543210'",
            "'091 9876543210',    '9876543210'",
            "'09876543210',       '9876543210'",
            "'9876543210',        '9876543210'",
            "'(098) 765-4321-0',  '9876543210'",
            "'+91-98765-43210',   '9876543210'",
    })
    void normalizesIndianPhoneFormats(String input, String expected) {
        assertEquals(expected, normalizer.normalize(input));
    }

    @Test
    void handlesNullAndBlank() {
        assertNull(normalizer.normalize(null));
        assertNull(normalizer.normalize(""));
        assertNull(normalizer.normalize("   "));
    }

    @Test
    void retainsInvalidPhones() {
        String result = normalizer.normalize("12345");
        assertEquals("12345", result);
        assertFalse(normalizer.isValid(result));
    }

    @Test
    void validatesCorrectPhones() {
        assertTrue(normalizer.isValid("9876543210"));
        assertFalse(normalizer.isValid("12345"));
        assertFalse(normalizer.isValid(null));
    }
}
