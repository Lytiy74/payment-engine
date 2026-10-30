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

    public void deposit(long amount) {
        balance = Math.addExact(balance, amount);
    }

    public void withdraw(long amount) {
        balance -= amount;
    }

}
