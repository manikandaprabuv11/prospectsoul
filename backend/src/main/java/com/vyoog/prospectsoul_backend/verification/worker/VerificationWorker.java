package com.vyoog.prospectsoul_backend.verification.worker;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.vyoog.prospectsoul_backend.verification.config.VerificationProperties;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationBatch;
import com.vyoog.prospectsoul_backend.verification.entity.VerificationBatchStatus;
import com.vyoog.prospectsoul_backend.verification.repository.VerificationBatchItemRepository;
import com.vyoog.prospectsoul_backend.verification.repository.VerificationBatchRepository;
import com.vyoog.prospectsoul_backend.verification.service.VerificationBatchStartedEvent;
import com.vyoog.prospectsoul_backend.verification.service.VerificationItemProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * The DB-backed verification queue worker.
 *
 * <p>There is no message broker, by design: the queue is the
 * {@code verification_batch_items} table, exactly as the existing import
 * pipeline uses its own rows (docs/dev_docs/14 §1, §9). That is what makes
 * progress survive a browser refresh and a backend restart — the only place
 * work-in-progress is recorded is PostgreSQL.
 *
 * <p>Each pass:
 * <ol>
 *   <li>returns items abandoned by a dead JVM to the queue;</li>
 *   <li>reconciles counters for every non-terminal batch;</li>
 *   <li>claims up to {@code batchSize} queued items with
 *       {@code FOR UPDATE SKIP LOCKED};</li>
 *   <li>processes each claimed item independently.</li>
 * </ol>
 */
@Component
@Slf4j
public class VerificationWorker {

    private final VerificationBatchItemRepository itemRepository;
    private final VerificationBatchRepository batchRepository;
    private final VerificationItemProcessor processor;
    private final VerificationProperties properties;

    /**
     * Proxy reference to this bean, so {@link #runOnce} reaches
     * {@link #claim} and {@link #recoverAbandonedItems} through Spring's
     * transaction proxy. This matters most for {@code claim()}: its
     * {@code FOR UPDATE SKIP LOCKED} row locks only survive until the status
     * update if both statements share one transaction, and a plain self-call
     * would give each its own.
     */
    private final VerificationWorker self;

    public VerificationWorker(VerificationBatchItemRepository itemRepository,
                              VerificationBatchRepository batchRepository,
                              VerificationItemProcessor processor,
                              VerificationProperties properties,
                              @Lazy VerificationWorker self) {
        this.itemRepository = itemRepository;
        this.batchRepository = batchRepository;
        this.processor = processor;
        this.properties = properties;
        this.self = self;
    }

    /**
     * The scheduled pass. This — not the after-commit kick below — is the
     * durability guarantee: whatever is QUEUED in the database is eventually
     * processed, whether it was queued a second ago or before the last
     * restart.
     */
    @Scheduled(
            initialDelayString = "${prospectsoul.verification.poll-interval-ms:5000}",
            fixedDelayString = "${prospectsoul.verification.poll-interval-ms:5000}")
    public void poll() {
        if (!properties.isWorkerEnabled()) {
            return;
        }
        runOnce();
    }

    /**
     * Starts work as soon as the creating transaction commits, so the analyst
     * sees the batch move without waiting a poll interval. Fired after commit
     * on purpose — before commit, the queue rows would not yet be visible to
     * the worker's own transaction.
     */
    @Async("verificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBatchStarted(VerificationBatchStartedEvent event) {
        if (!properties.isWorkerEnabled()) {
            return;
        }
        log.debug("verification batch={} kicking worker after commit", event.batchId());
        runOnce();
    }

    /**
     * One full pass. Package-visible and synchronous so tests can drive the
     * worker deterministically instead of waiting on the scheduler.
     *
     * @return number of items processed in this pass
     */
    public int runOnce() {
        try {
            self.recoverAbandonedItems();
            reconcileNonTerminalBatches();
        } catch (RuntimeException e) {
            log.error("verification worker recovery pass failed", e);
        }

        List<UUID> claimed;
        try {
            claimed = self.claim();
        } catch (RuntimeException e) {
            log.error("verification worker could not claim items", e);
            return 0;
        }

        // Per-item isolation: a failure here is recorded on that item and the
        // pass continues with the next one.
        int processed = 0;
        for (UUID itemId : claimed) {
            try {
                processor.process(itemId);
                processed++;
            } catch (RuntimeException e) {
                log.error("verification item={} failed outside the processor", itemId, e);
            }
        }
        return processed;
    }

    /**
     * Claims the next slice of work. The claim and the PROCESSING marking share
     * one transaction so the row lock taken by {@code FOR UPDATE SKIP LOCKED}
     * is still held when the status flips — without that, two workers could
     * claim the same row.
     */
    @Transactional
    public List<UUID> claim() {
        List<UUID> ids = itemRepository.claimQueuedItemIds(properties.resolvedBatchSize());
        if (!ids.isEmpty()) {
            itemRepository.markProcessing(ids);
        }
        return ids;
    }

    /**
     * Restart recovery. An item left PROCESSING has no live worker behind it
     * once the stale timeout has passed — the JVM that claimed it is gone — so
     * it returns to the queue with its attempt count intact, and the retry
     * budget still applies.
     */
    @Transactional
    public int recoverAbandonedItems() {
        Instant cutoff = Instant.now().minusMillis(properties.resolvedStaleItemTimeoutMs());
        int recovered = itemRepository.requeueStaleProcessingItems(cutoff);
        if (recovered > 0) {
            log.warn("verification worker re-queued {} item(s) abandoned before {}", recovered, cutoff);
        }
        return recovered;
    }

    /**
     * Brings every non-terminal batch's counters and status back in line with
     * its items. This is what closes a batch whose last item completed just as
     * the process died, and what makes {@code GET /verifications/active}
     * trustworthy after a restart.
     */
    public void reconcileNonTerminalBatches() {
        List<VerificationBatch> open = batchRepository.findByStatusIn(
                List.of(VerificationBatchStatus.QUEUED, VerificationBatchStatus.PROCESSING));
        for (VerificationBatch batch : open) {
            try {
                processor.refreshBatch(batch.getId(), batch.getRequestedBy());
            } catch (RuntimeException e) {
                log.error("verification batch={} could not be reconciled", batch.getId(), e);
            }
        }
    }
}
