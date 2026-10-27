package com.azaika;

import java.util.Objects;

public class Account {
    private final long id;
    private long balance;

    public Account(long id, long balance) {
        this.id = id;
        this.balance = balance;
    }

    public long getBalance() {
        return balance;
    }

    public long getId() {
        return id;
    }

    public long deposit(long amount){
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        balance += amount;
        return balance;
    }

    public long withdraw(long amount){
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (balance < amount) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        balance -= amount;
        return balance;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Account account)) return false;
        return id == account.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
