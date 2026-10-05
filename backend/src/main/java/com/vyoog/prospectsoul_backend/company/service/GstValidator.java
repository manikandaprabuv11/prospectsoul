package com.vyoog.prospectsoul_backend.company.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class GstValidator {

    private static final Pattern GST_FORMAT =
            Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");

    private static final String CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public record ValidationResult(boolean valid, String error) {
        public static ValidationResult ok() { return new ValidationResult(true, null); }
        public static ValidationResult fail(String error) { return new ValidationResult(false, error); }
    }

    public ValidationResult validate(String gst) {
        if (gst == null || gst.isBlank()) return ValidationResult.ok();

        String upper = gst.trim().toUpperCase();
        if (upper.length() != 15) {
            return ValidationResult.fail("GST number must be exactly 15 characters");
        }
        if (!GST_FORMAT.matcher(upper).matches()) {
            return ValidationResult.fail("GST number format is invalid");
        }
        char expected = computeCheckDigit(upper.substring(0, 14));
        if (upper.charAt(14) != expected) {
            return ValidationResult.fail("GST number check digit is invalid");
        }
        return ValidationResult.ok();
    }

    char computeCheckDigit(String first14) {
        int sum = 0;
        for (int i = 0; i < first14.length(); i++) {
            int codePoint = CHARSET.indexOf(first14.charAt(i));
            int factor = (i % 2 == 0) ? 1 : 2;
            int addend = factor * codePoint;
            addend = (addend / 36) + (addend % 36);
            sum += addend;
        }
        int remainder = sum % 36;
        int checkCodePoint = (36 - remainder) % 36;
        return CHARSET.charAt(checkCodePoint);
    }
}
