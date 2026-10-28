package com.azaika;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class Bank {

    private final Map<Long, Account> accounts;

    public Bank(int accountCount, long initialBalance) {
        this.accounts = new HashMap<>();
        init(accountCount, initialBalance);
    }

    public Optional<Account> findById(long id) {
        return Optional.ofNullable(accounts.get(id));
    }

    public long totalBalance() {
        return accounts
                .values()
                .stream()
                .mapToLong(Account::getBalance)
                .sum();
    }

    private void init(int accountCount, long initialBalance) {
        for (long i = 0; i < accountCount; i++) {
            accounts.put(i, new Account(i, initialBalance));
        }
    }
}