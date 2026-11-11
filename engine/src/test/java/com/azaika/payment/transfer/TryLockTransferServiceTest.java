package com.azaika.payment.transfer;

import com.azaika.payment.account.Account;
import com.azaika.payment.account.Bank;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Task 2.3: delete this line when you start the task")
class TryLockTransferServiceTest extends TransferConcurrencyContract {
    private static final Duration LOCK_TIMEOUT = Duration.ofMillis(200);

    @Override
    protected TransferService create(Bank bank) {
        return new TryLockTransferService(bank, Duration.ofMillis(5));
    }

    @Override
    protected Set<TransferResult> resultsAllowedUnderLoad() {
        return EnumSet.of(TransferResult.SUCCESS, TransferResult.INSUFFICIENT_FUNDS, TransferResult.ACCOUNT_BUSY);
    }

    @Test
    @Timeout(30)
    void transfer_whenSourceLockIsHeldElsewhere_returnsAccountBusyAfterTimeout() throws Exception {
        TransferService service = new TryLockTransferService(bank, LOCK_TIMEOUT);

        try (HeldLock ignored = HeldLock.on(account(1))) {
            assertBusyAfterTimeout(service);
        }
        assertEquals(INITIAL_BALANCE, account(1).getBalance());
        assertEquals(INITIAL_BALANCE, account(2).getBalance());
    }

    @Test
    @Timeout(30)
    void transfer_whenTargetLockIsHeldElsewhere_returnsAccountBusyAfterTimeout() throws Exception {
        TransferService service = new TryLockTransferService(bank, LOCK_TIMEOUT);

        try (HeldLock ignored = HeldLock.on(account(2))) {
            assertBusyAfterTimeout(service);
        }
        assertEquals(INITIAL_BALANCE, account(1).getBalance());
        assertEquals(INITIAL_BALANCE, account(2).getBalance());
    }

    @Test
    @Timeout(30)
    void transfer_afterAccountBusy_holdsNoLocks() throws Exception {
        TransferService service = new TryLockTransferService(bank, LOCK_TIMEOUT);

        try (HeldLock ignored = HeldLock.on(account(2))) {
            assertEquals(TransferResult.ACCOUNT_BUSY, service.transfer(1, 2, 100));
            assertTrue(isFree(account(1).lock()),
                    "The source lock is still held after ACCOUNT_BUSY: release what you took before giving up");
        }
    }

    @Test
    @Timeout(30)
    void transfer_afterSuccess_holdsNoLocks() throws Exception {
        TransferService service = new TryLockTransferService(bank, LOCK_TIMEOUT);

        assertEquals(TransferResult.SUCCESS, service.transfer(1, 2, 100));

        assertTrue(isFree(account(1).lock()), "The source lock is still held after a successful transfer");
        assertTrue(isFree(account(2).lock()), "The target lock is still held after a successful transfer");
    }

    @Test
    @Timeout(30)
    void transfer_afterBusyAccountIsReleased_succeeds() throws Exception {
        TransferService service = new TryLockTransferService(bank, LOCK_TIMEOUT);

        try (HeldLock ignored = HeldLock.on(account(1))) {
            assertEquals(TransferResult.ACCOUNT_BUSY, service.transfer(1, 2, 100));
        }

        assertEquals(TransferResult.SUCCESS, service.transfer(1, 2, 100));
        assertEquals(INITIAL_BALANCE - 100, account(1).getBalance());
        assertEquals(INITIAL_BALANCE + 100, account(2).getBalance());
    }

    private static void assertBusyAfterTimeout(TransferService service) {
        long start = System.nanoTime();
        TransferResult result = service.transfer(1, 2, 100);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertEquals(TransferResult.ACCOUNT_BUSY, result);
        assertTrue(elapsedMillis >= 150,
                "Gave up after " + elapsedMillis + " ms, but the lock timeout is " + LOCK_TIMEOUT.toMillis() + " ms");
        assertTrue(elapsedMillis < 5_000, "Waited " + elapsedMillis + " ms, far longer than the lock timeout");
    }

    /** Asks another thread whether the lock can be taken right now (the test thread itself could re-enter it). */
    private static boolean isFree(Lock lock) throws Exception {
        return CompletableFuture.supplyAsync(() -> {
            boolean acquired = lock.tryLock();
            if (acquired) {
                lock.unlock();
            }
            return acquired;
        }).get(5, TimeUnit.SECONDS);
    }

    /** Holds an account lock in a separate thread until closed. */
    private static final class HeldLock implements AutoCloseable {
        private final CountDownLatch release = new CountDownLatch(1);
        private final Thread holder;

        private HeldLock(Account account, CountDownLatch locked) {
            holder = new Thread(() -> {
                account.lock().lock();
                try {
                    locked.countDown();
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    account.lock().unlock();
                }
            }, "lock-holder");
            holder.setDaemon(true);
        }

        static HeldLock on(Account account) throws InterruptedException {
            CountDownLatch locked = new CountDownLatch(1);
            HeldLock held = new HeldLock(account, locked);
            held.holder.start();
            assertTrue(locked.await(5, TimeUnit.SECONDS), "The helper thread could not take the account lock");
            return held;
        }

        @Override
        public void close() {
            release.countDown();
            try {
                holder.join(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
