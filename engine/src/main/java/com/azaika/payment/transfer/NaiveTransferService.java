package com.azaika.payment.transfer;

import com.azaika.payment.account.Account;
import com.azaika.payment.account.Bank;

import java.util.Optional;

/**
 * Stage 1 baseline: correct in one thread, loses money under concurrent load (no locking at all).
 *
 * <p>Kept on purpose as the "before" picture: run the simulator with {@code --strategy=naive}.
 */
public final class NaiveTransferService implements TransferService {
    private final Bank bank;

    public NaiveTransferService(Bank bank) {
        this.bank = bank;
    }

    @Override
    public TransferResult transfer(long fromId, long toId, long amount) {
        if (amount <= 0) {
            return TransferResult.INVALID_AMOUNT;
        }

        if (fromId == toId) {
            return TransferResult.SAME_ACCOUNT;
        }

        Optional<Account> source = bank.findById(fromId);
        if (source.isEmpty()) {
            return TransferResult.ACCOUNT_NOT_FOUND;
        }

        Optional<Account> target = bank.findById(toId);
        if (target.isEmpty()) {
            return TransferResult.ACCOUNT_NOT_FOUND;
        }

        if (source.get().getBalance() < amount) {
            return TransferResult.INSUFFICIENT_FUNDS;
        }

        if (target.get().getBalance() > (Long.MAX_VALUE - amount)) {
            return TransferResult.BALANCE_OVERFLOW;
        }

        source.get().withdraw(amount);
        target.get().deposit(amount);

        return TransferResult.SUCCESS;
    }

}
