package com.vyoog.prospectsoul_backend.verification.mapper;

import java.time.Instant;

import com.vyoog.prospectsoul_backend.verification.entity.VerificationBatch;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationBatchStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Progress percentage and elapsed time shown on the Current Verification card. */
class VerificationProgressTest {

    @Test
    void progress_countsSkippedAsDoneSoABatchCanReach100() {
        assertThat(VerificationMapper.progressPercent(batch(100, 0, 0, 90, 5, 5))).isEqualTo(100);
    }

    @Test
    void progress_isTerminalItemsOverTotal() {
        // 65 verified + 2 failed + 1 skipped = 68 of 100
        assertThat(VerificationMapper.progressPercent(batch(100, 31, 1, 65, 2, 1))).isEqualTo(68);
    }

    @Test
    void progress_freshBatchIsZero() {
        assertThat(VerificationMapper.progressPercent(batch(10, 10, 0, 0, 0, 0))).isZero();
    }

    @Test
    void progress_emptyBatchIsComplete() {
        assertThat(VerificationMapper.progressPercent(batch(0, 0, 0, 0, 0, 0))).isEqualTo(100);
    }

    @Test
    void progress_neverExceeds100() {
        assertThat(VerificationMapper.progressPercent(batch(2, 0, 0, 3, 1, 0))).isEqualTo(100);
    }

    @Test
    void elapsedSeconds_isNullBeforeTheBatchStarts() {
        assertThat(VerificationMapper.elapsedSeconds(batch(10, 10, 0, 0, 0, 0))).isNull();
    }

    @Test
    void elapsedSeconds_usesCompletionTimeOnceFinished() {
        var batch = batch(1, 0, 0, 1, 0, 0);
        batch.setStartedAt(Instant.parse("2026-09-09T10:00:00Z"));
        batch.setCompletedAt(Instant.parse("2026-09-09T10:03:21Z"));

        assertThat(VerificationMapper.elapsedSeconds(batch)).isEqualTo(201);
    }

    @Test
    void elapsedSeconds_countsUpWhileStillRunning() {
        var batch = batch(10, 5, 1, 4, 0, 0);
        batch.setStartedAt(Instant.now().minusSeconds(30));

        assertThat(VerificationMapper.elapsedSeconds(batch)).isBetween(29L, 32L);
    }

    @Test
    void batchStatus_terminalityMatchesTheLifecycle() {
        assertThat(VerificationBatchStatus.QUEUED.isTerminal()).isFalse();
        assertThat(VerificationBatchStatus.PROCESSING.isTerminal()).isFalse();
        assertThat(VerificationBatchStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(VerificationBatchStatus.COMPLETED_WITH_ERRORS.isTerminal()).isTrue();
        assertThat(VerificationBatchStatus.FAILED.isTerminal()).isTrue();
        assertThat(VerificationBatchStatus.CANCELLED.isTerminal()).isTrue();
    }

    private static VerificationBatch batch(int total, int queued, int processing,
                                            int verified, int failed, int skipped) {
        return VerificationBatch.builder()
                .requestedBy("actor")
                .totalCount(total)
                .queuedCount(queued)
                .processingCount(processing)
                .verifiedCount(verified)
                .failedCount(failed)
                .skippedCount(skipped)
                .build();
    }
}
