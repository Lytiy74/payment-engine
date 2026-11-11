package com.azaika.payment.processing;

import com.azaika.payment.transfer.TransferResult;
import com.azaika.payment.transfer.TransferService;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Processes transfers asynchronously: requests wait in a bounded queue and a fixed number of
 * worker threads execute them through the given {@link TransferService}.
 *
 * <p>Tasks 4.1 (queue and workers), 4.2 (backpressure), 4.3 (graceful shutdown). See ROADMAP.md.
 */
public final class PaymentProcessor {
    private final TransferService service;
    private final int workers;
    private final int queueCapacity;

    /**
     * @param service       executes the transfers
     * @param workers       number of worker threads
     * @param queueCapacity how many requests may wait in the queue (not counting the ones being executed)
     */
    public PaymentProcessor(TransferService service, int workers, int queueCapacity) {
        this.service = service;
        this.workers = workers;
        this.queueCapacity = queueCapacity;
    }

    /**
     * Accepts a request and returns immediately; never blocks the caller.
     *
     * <p>The future completes with the result of the transfer once a worker has executed it.
     * It completes exceptionally if the service throws. It is already completed with
     * {@link TransferResult#QUEUE_FULL} when the queue has no room (task 4.2) and with
     * {@link TransferResult#SHUTTING_DOWN} after {@link #shutdown} was called (task 4.3).
     */
    public CompletableFuture<TransferResult> submit(TransferRequest request) {
        throw new UnsupportedOperationException("Task 4.1: PaymentProcessor.submit is not implemented yet");
    }

    /**
     * Stops accepting new requests, lets the workers finish everything already accepted
     * and then stops the worker threads. Safe to call more than once.
     *
     * @return {@code true} if all accepted requests were finished within the timeout
     */
    public boolean shutdown(Duration timeout) throws InterruptedException {
        throw new UnsupportedOperationException("Task 4.3: PaymentProcessor.shutdown is not implemented yet");
    }
}
