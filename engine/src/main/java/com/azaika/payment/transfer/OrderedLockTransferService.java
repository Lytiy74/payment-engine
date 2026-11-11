package com.azaika.payment.transfer;

import com.azaika.payment.account.Bank;

/**
 * Correct under concurrent load with one lock per account, and free of deadlock thanks to lock ordering.
 *
 * <p>Task 2.2. See ROADMAP.md.
 */
public final class OrderedLockTransferService implements TransferService {
    private final Bank bank;

    public OrderedLockTransferService(Bank bank) {
        this.bank = bank;
    }

    @Override
    public TransferResult transfer(long fromId, long toId, long amount) {
        throw new UnsupportedOperationException("Task 2.2: OrderedLockTransferService is not implemented yet");
    }
}
