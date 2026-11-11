package com.azaika.payment.transfer;

import com.azaika.payment.account.Bank;
import com.azaika.payment.testutil.Concurrently;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Task 3.3: delete this line when you start the task")
class AuditableTransferServiceTest extends TransferConcurrencyContract {

    @Override
    protected TransferService create(Bank bank) {
        return new AuditableTransferService(bank);
    }

    @Test
    void totalBalance_withoutTransfers_equalsBankTotal() {
        AuditableTransferService service = new AuditableTransferService(bank);

        assertEquals(INITIAL_BALANCE * ACCOUNT_COUNT, service.totalBalance());
    }

    @Test
    @Timeout(120)
    void totalBalance_whileTransfersRun_isAlwaysExact() {
        int accounts = 50;
        long balance = 1_000;
        long expected = accounts * balance;
        Bank bigBank = new Bank(accounts, balance);
        AuditableTransferService service = new AuditableTransferService(bigBank);
        AtomicBoolean stop = new AtomicBoolean();
        AtomicLong audits = new AtomicLong();

        // Thread 0 audits for two seconds, the other four keep moving money.
        Concurrently.run(5, Duration.ofSeconds(60), thread -> {
            if (thread == 0) {
                try {
                    long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
                    while (System.nanoTime() < deadline) {
                        long total = service.totalBalance();
                        if (total != expected) {
                            throw new AssertionError("Audit #" + audits.get() + " saw " + total + " instead of "
                                    + expected + ": it caught a transfer between withdraw and deposit");
                        }
                        audits.incrementAndGet();
                    }
                } finally {
                    stop.set(true);
                }
            } else {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                while (!stop.get()) {
                    long from = random.nextInt(accounts);
                    long to = (from + 1 + random.nextInt(accounts - 1)) % accounts;
                    service.transfer(from, to, 1 + random.nextInt(100));
                }
            }
        });

        assertTrue(audits.get() > 0, "No audit finished");
        assertEquals(expected, bigBank.totalBalance());
    }
}
