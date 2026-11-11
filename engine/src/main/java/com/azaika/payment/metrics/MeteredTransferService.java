package com.azaika.payment.metrics;

import com.azaika.payment.transfer.TransferResult;
import com.azaika.payment.transfer.TransferService;

/**
 * A decorator: delegates every transfer and records its result in {@link TransferMetrics}.
 *
 * <p>Task 3.1. See ROADMAP.md.
 */
public final class MeteredTransferService implements TransferService {
    private final TransferService delegate;
    private final TransferMetrics metrics;

    public MeteredTransferService(TransferService delegate, TransferMetrics metrics) {
        this.delegate = delegate;
        this.metrics = metrics;
    }

    @Override
    public TransferResult transfer(long fromId, long toId, long amount) {
        throw new UnsupportedOperationException("Task 3.1: MeteredTransferService is not implemented yet");
    }
}
