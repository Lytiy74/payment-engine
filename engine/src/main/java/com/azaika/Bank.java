package com.azaika;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Bank {

    private final List<Account> accounts;

    public Bank() {
        this.accounts = new ArrayList<>();
        init();
    }

    public Optional<Account> findById(int id) {
        return Optional.of(accounts.get(id));
    }

    public long totalBalance() {
        return accounts
                .stream()
                .mapToLong(Account::getBalance)
                .sum();
    }

    private void init() {
        for (int i = 0; i < 10; i++) {
            accounts.add(new Account(i, 100_000));
        }
    }
}