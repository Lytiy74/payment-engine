package com.azaika.payment.account;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A bank account. Money is stored in minor units (kopecks) as {@code long}.
 *
 * <p>This class is NOT thread-safe on its own: balance operations are plain reads and writes.
 * Locking is the job of a {@code TransferService} strategy.
 */
public final class Account {
    private final long id;
    private final Lock lock = new ReentrantLock();
    private long balance;

    public Account(long id, long balance) {
        this.id = id;
        if (balance < 0) {
            throw new IllegalArgumentException("Balance must be non-negative");
        }
        this.balance = balance;
    }

    public long getBalance() {
        return balance;
    }

    public long getId() {
        return id;
    }

    /**
     * The explicit lock of this account.
     *
     * <p>Not used before task 2.3. Strategies built on {@code synchronized} lock on the account object itself.
     */
    public Lock lock() {
        return lock;
    }

    public void deposit(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        balance = Math.addExact(balance, amount);
    }

    public void withdraw(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (amount > balance) {
            throw new IllegalStateException("Insufficient funds");
        }
        balance -= amount;
    }

    @Override
    public String toString() {
        return "Account{" +
                "balance=" + balance +
                ", id=" + id +
                '}';
    }
}
