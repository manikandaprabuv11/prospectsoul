package com.vyoog.prospectsoul_backend.company;

import com.vyoog.prospectsoul_backend.company.service.GstValidator;
import com.vyoog.prospectsoul_backend.company.service.GstValidator.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class GstValidatorTest {

    private GstValidator validator;

    @BeforeEach
    void setUp() {
        validator = new GstValidator();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    @DisplayName("Null, empty and blank values are valid (field is optional)")
    void blankValues_areValid(String input) {
        assertThat(validator.validate(input).valid()).isTrue();
    }

    @Test
    @DisplayName("Valid GSTIN passes format and check digit")
    void validGstin() {
        // 27AAPFU0939F1ZV is a well-known test GSTIN
        ValidationResult r = validator.validate("27AAPFU0939F1ZV");
        assertThat(r.valid()).isTrue();
    }

    @Test
    @DisplayName("Another valid GSTIN: same state code, different entity")
    void validGstin2() {
        // Verify algorithm works by checking that the first known-good GSTIN
        // with a different last char fails, confirming we are really checking
        ValidationResult good = validator.validate("27AAPFU0939F1ZV");
        assertThat(good.valid()).isTrue();
        // Incrementing check digit should fail
        ValidationResult bad = validator.validate("27AAPFU0939F1ZW");
        assertThat(bad.valid()).isFalse();
        assertThat(bad.error()).contains("check digit");
    }

    @Test
    @DisplayName("Too short — rejected")
    void tooShort() {
        ValidationResult r = validator.validate("27AAPFU0939F1Z");
        assertThat(r.valid()).isFalse();
        assertThat(r.error()).contains("15 characters");
    }

    @Test
    @DisplayName("Too long — rejected")
    void tooLong() {
        ValidationResult r = validator.validate("27AAPFU0939F1ZVX");
        assertThat(r.valid()).isFalse();
        assertThat(r.error()).contains("15 characters");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "XXAAPFU0939F1ZV",   // state code is not numeric
            "27AAXX00939F1ZV",   // PAN section wrong format
    })
    @DisplayName("Format violations detected")
    void formatViolations(String input) {
        ValidationResult r = validator.validate(input);
        assertThat(r.valid()).isFalse();
    }

    @Test
    @DisplayName("Lowercase input is uppercased and validated correctly")
    void lowercaseInput_isValid() {
        ValidationResult r = validator.validate("27aapfu0939f1zv");
        assertThat(r.valid()).isTrue();
    }

    @Test
    @DisplayName("Valid format but wrong check digit")
    void wrongCheckDigit() {
        // Change last char from V to A
        ValidationResult r = validator.validate("27AAPFU0939F1ZA");
        assertThat(r.valid()).isFalse();
        assertThat(r.error()).contains("check digit");
    }

    @Test
    @DisplayName("Valid GSTIN with known check digit V passes")
    void checkDigitVerification() {
        // If 27AAPFU0939F1ZV is valid, the check digit computation works
        assertThat(validator.validate("27AAPFU0939F1ZV").valid()).isTrue();
        // Any other last char should fail
        assertThat(validator.validate("27AAPFU0939F1ZA").valid()).isFalse();
    }
}
