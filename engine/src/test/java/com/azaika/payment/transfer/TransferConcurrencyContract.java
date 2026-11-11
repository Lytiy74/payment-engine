package com.azaika.payment.transfer;

import com.azaika.payment.testutil.Concurrently;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a thread-safe {@link TransferService} must guarantee under concurrent load:
 * money is never lost or created, no account is overdrawn, and transfers never deadlock.
 */
abstract class TransferConcurrencyContract extends TransferFunctionalContract {
    private static final int THREADS = 8;
    private static final int TRANSFERS_PER_THREAD = 50_000;
    private static final Duration RUN_TIME = Duration.ofSeconds(2);
    private static final Duration WAIT = Duration.ofSeconds(30);

    /** Results a strategy may return when threads compete for the same accounts. */
    protected Set<TransferResult> resultsAllowedUnderLoad() {
        return EnumSet.of(TransferResult.SUCCESS, TransferResult.INSUFFICIENT_FUNDS);
    }

    @Test
    @Timeout(120)
    void randomTransfers_fromManyThreads_keepTotalAndNeverOverdraw() {
        Map<TransferResult, LongAdder> counts = newCounts();

        Concurrently.run(THREADS, WAIT, thread -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < TRANSFERS_PER_THREAD; i++) {
                long from = random.nextInt(ACCOUNT_COUNT);
                long to = (from + 1 + random.nextInt(ACCOUNT_COUNT - 1)) % ACCOUNT_COUNT;
                long amount = 1 + random.nextInt(50_000);
                counts.get(transferService.transfer(from, to, amount)).increment();
            }
        });

        assertMoneyIsSafe(counts);
    }

    @Test
    @Timeout(120)
    void oppositeTransfers_doNotDeadlock() {
        Map<TransferResult, LongAdder> counts = newCounts();

        // Threads 0 and 2 send 0 -> 1, threads 1 and 3 send 1 -> 0: the classic deadlock recipe.
        Concurrently.run(4, WAIT, thread -> {
            long from = thread % 2;
            long to = 1 - from;
            long deadline = System.nanoTime() + RUN_TIME.toNanos();
            while (System.nanoTime() < deadline) {
                counts.get(transferService.transfer(from, to, 1)).increment();
            }
        });

        assertMoneyIsSafe(counts);
    }

    @Test
    @Timeout(120)
    void ringTransfers_doNotDeadlock() {
        Map<TransferResult, LongAdder> counts = newCounts();
        int ring = 5;

        // Thread i sends i -> i + 1, the last one closes the ring: 4 -> 0.
        Concurrently.run(ring, WAIT, thread -> {
            long from = thread;
            long to = (thread + 1) % ring;
            long deadline = System.nanoTime() + RUN_TIME.toNanos();
            while (System.nanoTime() < deadline) {
                counts.get(transferService.transfer(from, to, 1)).increment();
            }
        });

        assertMoneyIsSafe(counts);
    }

    private void assertMoneyIsSafe(Map<TransferResult, LongAdder> counts) {
        assertEquals(INITIAL_BALANCE * ACCOUNT_COUNT, bank.totalBalance(),
                "The sum of all balances changed: money was lost or created");
        for (long id = 0; id < ACCOUNT_COUNT; id++) {
            assertTrue(account(id).getBalance() >= 0, "Account " + id + " is overdrawn: " + account(id).getBalance());
        }
        Set<TransferResult> allowed = resultsAllowedUnderLoad();
        for (TransferResult result : TransferResult.values()) {
            long count = counts.get(result).sum();
            assertTrue(count == 0 || allowed.contains(result),
                    "Unexpected result under load: " + result + " x " + count);
        }
        assertTrue(counts.get(TransferResult.SUCCESS).sum() > 0, "No transfer succeeded at all");
    }

    private static Map<TransferResult, LongAdder> newCounts() {
        Map<TransferResult, LongAdder> counts = new EnumMap<>(TransferResult.class);
        for (TransferResult result : TransferResult.values()) {
            counts.put(result, new LongAdder());
        }
        return counts;
    }
}
