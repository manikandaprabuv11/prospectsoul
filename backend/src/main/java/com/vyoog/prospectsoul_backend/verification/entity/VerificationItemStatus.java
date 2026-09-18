package com.vyoog.prospectsoul_backend.verification.entity;

/**
 * Item lifecycle. {@code QUEUED} and {@code PROCESSING} are the only
 * non-terminal states, so "is this company already being verified?" is exactly
 * "does it have an item in one of those two states?".
 */
public enum VerificationItemStatus {
    QUEUED,
    PROCESSING,
    VERIFIED,
    FAILED,
    SKIPPED;

    public boolean isTerminal() {
        return this != QUEUED && this != PROCESSING;
    }
}
