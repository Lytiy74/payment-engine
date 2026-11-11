package com.azaika.payment.transfer;

import com.azaika.payment.account.Bank;

import java.time.Duration;

/**
 * Per-account {@link com.azaika.payment.account.Account#lock() explicit locks} taken with a timeout:
 * if both locks are not acquired within {@code lockTimeout}, the transfer gives up with
 * {@link TransferResult#ACCOUNT_BUSY} and holds no locks afterwards.
 *
 * <p>Task 2.3. See ROADMAP.md.
 */
public final class TryLockTransferService implements TransferService {
    private final Bank bank;
    private final Duration lockTimeout;

    public TryLockTransferService(Bank bank, Duration lockTimeout) {
        this.bank = bank;
        this.lockTimeout = lockTimeout;
    }

    @Override
    public TransferResult transfer(long fromId, long toId, long amount) {
        throw new UnsupportedOperationException("Task 2.3: TryLockTransferService is not implemented yet");
    }
}
