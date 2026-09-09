package com.vyoog.prospectsoul_backend.imports.normalization;

import org.springframework.stereotype.Component;

@Component
public class PhoneNormalizer {

    public String normalize(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("[^0-9]", "");

        if (digits.startsWith("91") && digits.length() == 12) {
            digits = digits.substring(2);
        } else if (digits.startsWith("091") && digits.length() == 13) {
            digits = digits.substring(3);
        } else if (digits.startsWith("0091") && digits.length() == 14) {
            digits = digits.substring(4);
        } else if (digits.startsWith("0") && digits.length() == 11) {
            digits = digits.substring(1);
        }

        return digits;
    }

    public boolean isValid(String normalizedPhone) {
        if (normalizedPhone == null || normalizedPhone.isBlank()) {
            return false;
        }
        return normalizedPhone.length() == 10 && normalizedPhone.matches("\\d{10}");
    }
}
