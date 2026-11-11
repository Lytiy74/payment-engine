package com.azaika.payment.metrics;

import com.azaika.payment.transfer.TransferResult;

/**
 * Counts transfer outcomes. Safe to call from many threads, and exact: no update is ever lost.
 *
 * <p>Task 3.1. See ROADMAP.md.
 */
public final class TransferMetrics {

    /** Adds one occurrence of the given result. */
    public void record(TransferResult result) {
        throw new UnsupportedOperationException("Task 3.1: TransferMetrics is not implemented yet");
    }

    /** How many times the given result was recorded. */
    public long count(TransferResult result) {
        throw new UnsupportedOperationException("Task 3.1: TransferMetrics is not implemented yet");
    }

    /** How many results were recorded in total. */
    public long total() {
        throw new UnsupportedOperationException("Task 3.1: TransferMetrics is not implemented yet");
    }
}
