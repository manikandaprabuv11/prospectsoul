package com.vyoog.prospectsoul_backend.company.phone.service;

import com.vyoog.prospectsoul_backend.company.phone.entity.PhoneType;

public record PhoneNormalizationResult(
        String normalized,
        PhoneType phoneType,
        boolean valid
) {
    public static PhoneNormalizationResult classify(String normalized) {
        if (normalized == null || normalized.length() != 10 || !normalized.matches("\\d{10}")) {
            return new PhoneNormalizationResult(normalized, PhoneType.INVALID, false);
        }
        char first = normalized.charAt(0);
        if (first >= '6' && first <= '9') {
            return new PhoneNormalizationResult(normalized, PhoneType.MOBILE, true);
        }
        return new PhoneNormalizationResult(normalized, PhoneType.LANDLINE, true);
    }
}
