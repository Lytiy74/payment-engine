package com.azaika.payment.metrics;

import com.azaika.payment.testutil.Concurrently;
import com.azaika.payment.transfer.TransferResult;
import com.azaika.payment.transfer.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled("Task 3.1: delete this line when you start the task")
class TransferMetricsTest {
    private static final int THREADS = 8;
    private static final int CALLS_PER_THREAD = 1_000_000;

    private TransferMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new TransferMetrics();
    }

    @Test
    void count_beforeAnythingIsRecorded_isZero() {
        for (TransferResult result : TransferResult.values()) {
            assertEquals(0L, metrics.count(result));
        }
        assertEquals(0L, metrics.total());
    }

    @Test
    void record_countsEachResultSeparately() {
        metrics.record(TransferResult.SUCCESS);
        metrics.record(TransferResult.SUCCESS);
        metrics.record(TransferResult.INSUFFICIENT_FUNDS);

        assertEquals(2L, metrics.count(TransferResult.SUCCESS));
        assertEquals(1L, metrics.count(TransferResult.INSUFFICIENT_FUNDS));
        assertEquals(0L, metrics.count(TransferResult.ACCOUNT_BUSY));
        assertEquals(3L, metrics.total());
    }

    @Test
    @Timeout(60)
    void record_fromManyThreads_losesNoUpdate() {
        Concurrently.run(THREADS, Duration.ofSeconds(30), thread -> {
            for (int i = 0; i < CALLS_PER_THREAD; i++) {
                metrics.record(i % 2 == 0 ? TransferResult.SUCCESS : TransferResult.INSUFFICIENT_FUNDS);
            }
        });

        long half = (long) THREADS * CALLS_PER_THREAD / 2;
        assertEquals(half, metrics.count(TransferResult.SUCCESS));
        assertEquals(half, metrics.count(TransferResult.INSUFFICIENT_FUNDS));
        assertEquals(2 * half, metrics.total());
    }

    @Test
    void meteredService_returnsWhatTheDelegateReturns() {
        TransferService delegate = (from, to, amount) -> amount > 100
                ? TransferResult.INSUFFICIENT_FUNDS
                : TransferResult.SUCCESS;
        TransferService metered = new MeteredTransferService(delegate, metrics);

        assertEquals(TransferResult.SUCCESS, metered.transfer(1, 2, 100));
        assertEquals(TransferResult.INSUFFICIENT_FUNDS, metered.transfer(1, 2, 101));
    }

    @Test
    void meteredService_recordsEveryResult() {
        TransferService delegate = (from, to, amount) -> amount > 100
                ? TransferResult.INSUFFICIENT_FUNDS
                : TransferResult.SUCCESS;
        TransferService metered = new MeteredTransferService(delegate, metrics);

        metered.transfer(1, 2, 50);
        metered.transfer(1, 2, 60);
        metered.transfer(1, 2, 500);

        assertEquals(2L, metrics.count(TransferResult.SUCCESS));
        assertEquals(1L, metrics.count(TransferResult.INSUFFICIENT_FUNDS));
        assertEquals(3L, metrics.total());
    }

    @Test
    @Timeout(60)
    void meteredService_fromManyThreads_losesNoUpdate() {
        TransferService metered = new MeteredTransferService((from, to, amount) -> TransferResult.SUCCESS, metrics);

        Concurrently.run(THREADS, Duration.ofSeconds(30), thread -> {
            for (int i = 0; i < CALLS_PER_THREAD; i++) {
                metered.transfer(1, 2, 1);
            }
        });

        assertEquals((long) THREADS * CALLS_PER_THREAD, metrics.count(TransferResult.SUCCESS));
    }
}
