package com.azaika.payment.transfer;

import com.azaika.payment.account.Bank;

/**
 * Correct under concurrent load: every transfer runs under one shared lock.
 *
 * <p>Task 2.1. See ROADMAP.md.
 */
public final class GlobalLockTransferService implements TransferService {
    private final Bank bank;

    public GlobalLockTransferService(Bank bank) {
        this.bank = bank;
    }

    @Override
    public TransferResult transfer(long fromId, long toId, long amount) {
        throw new UnsupportedOperationException("Task 2.1: GlobalLockTransferService is not implemented yet");
    }
}
