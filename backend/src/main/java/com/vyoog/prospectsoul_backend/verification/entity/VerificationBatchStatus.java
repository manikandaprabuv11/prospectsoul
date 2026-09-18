package com.vyoog.prospectsoul_backend.verification.entity;

/** Batch lifecycle per docs/dev_docs/13 §8. */
public enum VerificationBatchStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    COMPLETED_WITH_ERRORS,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this != QUEUED && this != PROCESSING;
    }
}
