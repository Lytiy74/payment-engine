package com.azaika.payment.transfer;

import com.azaika.payment.account.Account;
import com.azaika.payment.account.Bank;

import java.util.Optional;

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
    public synchronized TransferResult transfer(long fromId, long toId, long amount) {
        if (amount <= 0) {
            return TransferResult.INVALID_AMOUNT;
        }

        if (fromId == toId) {
            return TransferResult.SAME_ACCOUNT;
        }

        Optional<Account> fromOptional = bank.findById(fromId);
        Optional<Account> toOptional = bank.findById(toId);

        if (fromOptional.isEmpty() || toOptional.isEmpty()) {
            return TransferResult.ACCOUNT_NOT_FOUND;
        }

        Account from = fromOptional.get();
        Account to = toOptional.get();

        if (from.getBalance() < amount) {
            return TransferResult.INSUFFICIENT_FUNDS;
        }

        try {
            to.deposit(amount);
            from.withdraw(amount);
        } catch (ArithmeticException e) {
            return TransferResult.BALANCE_OVERFLOW;
        }

        return TransferResult.SUCCESS;
    }
}
