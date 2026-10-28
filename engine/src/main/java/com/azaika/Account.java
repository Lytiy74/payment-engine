package com.azaika;

public final class Account {
    private final long id;
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

    public void transfer(long amount, Account target) {
        if (target == null) {
            throw new IllegalArgumentException("Target account must not be null");
        }
        if (target == this) {
            throw new IllegalArgumentException("Cannot transfer to self");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (balance < amount) {
            throw new IllegalStateException("Insufficient balance");
        }
        long newBalance = balance - amount;
        target.balance = Math.addExact(target.balance, amount);
        this.balance = newBalance;
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
        if (balance < amount) {
            throw new IllegalStateException("Insufficient balance");
        }
        balance -= amount;
    }

}
