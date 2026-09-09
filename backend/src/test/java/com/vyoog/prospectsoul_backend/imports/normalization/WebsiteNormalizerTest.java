package com.vyoog.prospectsoul_backend.imports.normalization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WebsiteNormalizerTest {

    private final WebsiteNormalizer normalizer = new WebsiteNormalizer();

    @ParameterizedTest
    @CsvSource({
            "'https://www.example.com/about',  'example.com'",
            "'http://www.example.com',          'example.com'",
            "'www.example.com',                 'example.com'",
            "'example.com',                     'example.com'",
            "'HTTPS://WWW.EXAMPLE.COM/',        'example.com'",
            "'http://example.com?q=1',          'example.com'",
            "'example.com#section',             'example.com'",
            "'http://sub.example.com/path',     'sub.example.com'",
    })
    void normalizesVariousUrlFormats(String input, String expected) {
        assertEquals(expected, normalizer.normalize(input));
    }

    @Test
    void handlesNullAndBlank() {
        assertNull(normalizer.normalize(null));
        assertNull(normalizer.normalize(""));
        assertNull(normalizer.normalize("   "));
    }
}
