package com.azaika.payment.account;

import com.azaika.payment.testutil.Concurrently;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Task 3.2: delete this line when you start the task")
class BankOpenAccountTest {
    private static final int ACCOUNT_COUNT = 10;
    private static final long INITIAL_BALANCE = 100_000;

    private Bank bank;

    @BeforeEach
    void setUp() {
        bank = new Bank(ACCOUNT_COUNT, INITIAL_BALANCE);
    }

    @Test
    void openAccount_returnsAccountWithGivenBalance() {
        Account account = bank.openAccount(500);

        assertEquals(500L, account.getBalance());
    }

    @Test
    void openAccount_givesIdsNotUsedBefore() {
        Account first = bank.openAccount(1);
        Account second = bank.openAccount(1);

        assertTrue(first.getId() >= ACCOUNT_COUNT, "Id " + first.getId() + " belongs to an existing account");
        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void openAccount_makesAccountFindableAndCounted() {
        Account account = bank.openAccount(500);

        assertSame(account, bank.findById(account.getId()).orElseThrow());
        assertEquals(ACCOUNT_COUNT + 1, bank.accountCount());
        assertEquals(INITIAL_BALANCE * ACCOUNT_COUNT + 500, bank.totalBalance());
    }

    @Test
    void openAccount_negativeBalance_throwsAndAddsNothing() {
        assertThrows(IllegalArgumentException.class, () -> bank.openAccount(-1));
        assertEquals(ACCOUNT_COUNT, bank.accountCount());
    }

    @Test
    @Timeout(60)
    void openAccount_fromManyThreads_losesNoAccountAndRepeatsNoId() {
        int threads = 16;
        int perThread = 2_000;
        Set<Long> ids = ConcurrentHashMap.newKeySet();

        Concurrently.run(threads, Duration.ofSeconds(30), thread -> {
            for (int i = 0; i < perThread; i++) {
                ids.add(bank.openAccount(10).getId());
            }
        });

        int opened = threads * perThread;
        assertEquals(opened, ids.size(), "Some ids were handed out twice");
        assertEquals(ACCOUNT_COUNT + opened, bank.accountCount(), "Some accounts were lost");
        assertEquals(INITIAL_BALANCE * ACCOUNT_COUNT + 10L * opened, bank.totalBalance());
        for (long id : ids) {
            assertTrue(bank.findById(id).isPresent(), "Account " + id + " was opened but cannot be found");
        }
    }

    @Test
    @Timeout(60)
    void readers_whileAccountsAreOpened_neverFail() {
        AtomicBoolean stop = new AtomicBoolean();

        // Threads 0 and 1 open accounts, the other four read the registry at the same time.
        Concurrently.run(6, Duration.ofSeconds(30), thread -> {
            if (thread < 2) {
                try {
                    for (int i = 0; i < 20_000; i++) {
                        bank.openAccount(1);
                    }
                } finally {
                    stop.set(true);
                }
            } else {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                while (!stop.get()) {
                    bank.findById(random.nextInt(40_000));
                    bank.totalBalance();
                    bank.accountCount();
                }
            }
        });

        assertEquals(ACCOUNT_COUNT + 40_000, bank.accountCount());
    }
}
