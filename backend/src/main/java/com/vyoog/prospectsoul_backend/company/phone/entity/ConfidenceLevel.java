package com.vyoog.prospectsoul_backend.company.phone.entity;

public enum ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW;

    public boolean isHigherThan(ConfidenceLevel other) {
        if (other == null) return true;
        return this.ordinal() < other.ordinal();
    }

    public static ConfidenceLevel max(ConfidenceLevel a, ConfidenceLevel b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.ordinal() <= b.ordinal() ? a : b;
    }
}
