package com.azaika.payment.processing;

import com.azaika.payment.transfer.TransferResult;
import com.azaika.payment.transfer.TransferService;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** A {@link TransferService} for processor tests: it moves no money, it only remembers how it was called. */
final class FakeTransferService implements TransferService {
    private final AtomicInteger calls = new AtomicInteger();
    private final Set<Thread> threads = ConcurrentHashMap.newKeySet();
    private final CountDownLatch firstCallStarted = new CountDownLatch(1);
    private final CountDownLatch released;
    private final TransferResult result;

    private FakeTransferService(TransferResult result, boolean blocked) {
        this.result = result;
        this.released = new CountDownLatch(blocked ? 1 : 0);
    }

    /** Every call returns the given result at once. */
    static FakeTransferService returning(TransferResult result) {
        return new FakeTransferService(result, false);
    }

    /** Every call waits until {@link #release()} and then returns SUCCESS. */
    static FakeTransferService blocked() {
        return new FakeTransferService(TransferResult.SUCCESS, true);
    }

    @Override
    public TransferResult transfer(long fromId, long toId, long amount) {
        threads.add(Thread.currentThread());
        firstCallStarted.countDown();
        try {
            released.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        calls.incrementAndGet();
        return result;
    }

    void release() {
        released.countDown();
    }

    boolean awaitFirstCall() throws InterruptedException {
        return firstCallStarted.await(5, TimeUnit.SECONDS);
    }

    /** How many calls have finished. */
    int calls() {
        return calls.get();
    }

    /** The threads that executed transfers. */
    Set<Thread> threads() {
        return threads;
    }
}
