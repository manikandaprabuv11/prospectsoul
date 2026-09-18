package com.vyoog.prospectsoul_backend.verification.service;

import java.util.UUID;

/**
 * Published after the batch-creation transaction commits, so the worker can
 * begin immediately instead of waiting for the next scheduled poll.
 *
 * <p>This is a latency optimisation only. The scheduled worker pass is the
 * guarantee: if the event is never delivered — the JVM dies between commit and
 * dispatch, say — the queued rows are still in the database and the next poll
 * (this instance's or another's) picks them up.
 */
public record VerificationBatchStartedEvent(UUID batchId) {}
