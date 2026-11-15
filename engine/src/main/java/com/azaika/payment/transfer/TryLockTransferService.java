package com.azaika.payment.transfer;

import com.azaika.payment.account.Account;
import com.azaika.payment.account.Bank;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

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
        Account first = from.getId() < to.getId() ? from : to;
        Account second = first == from ? to : from;

        try {
            if (!acquire(first.lock())) {
                return TransferResult.ACCOUNT_BUSY;
            }
            try {
                if (!acquire(second.lock())) {
                    return TransferResult.ACCOUNT_BUSY;
                }
                try {
                    return transfer(from, to, amount);
                } finally {
                    second.lock().unlock();
                }
            } finally {
                first.lock().unlock();
            }
        } catch (
                InterruptedException e) {
            Thread.currentThread().interrupt();
            return TransferResult.SHUTTING_DOWN;
        }
    }

    private boolean acquire(Lock lock) throws InterruptedException {
        return lock.tryLock(lockTimeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    private TransferResult transfer(Account from, Account to, long amount) {
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
