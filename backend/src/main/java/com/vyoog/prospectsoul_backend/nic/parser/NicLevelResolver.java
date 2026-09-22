package com.vyoog.prospectsoul_backend.nic.parser;

/**
 * Level is derived from the numeric length of the NIC code — 1 (Section)
 * to 5 (Sub-class). The classification is defined in Domain Model Addendum
 * §2, and the DB check_constraint on nic_codes.level enforces the range.
 */
public final class NicLevelResolver {

    private NicLevelResolver() {}

    public static short forCode(String code) {
        if (code == null) throw new IllegalArgumentException("code is null");
        String trimmed = code.trim();
        if (trimmed.isEmpty() || trimmed.length() > 5 || !trimmed.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("code must be 1-5 digits: '" + code + "'");
        }
        return (short) trimmed.length();
    }
}
