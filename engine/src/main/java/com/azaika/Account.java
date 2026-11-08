package com.azaika;

import java.util.concurrent.atomic.LongAdder;

public final class Account {
    private final long id;
    private long balance;
    public final LongAdder negativeBalanceOccuranceCounter = new LongAdder();

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

    public void deposit(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        balance = Math.addExact(balance, amount);
    }

    public void withdraw(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        if (amount > balance) {
            throw new IllegalStateException("Insufficient funds");
        }
        balance -= amount;
        if (balance < 0) {
            negativeBalanceOccuranceCounter.increment();
        }
    }

    @Override
    public String toString() {
        return "Account{" +
                "balance=" + balance +
                ", id=" + id +
                '}';
    }
}
