package com.azaika.payment.account;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The registry of accounts.
 *
 * <p>Accounts created by the constructor get ids {@code 0 .. accountCount - 1}.
 */
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

    public int accountCount() {
        return accounts.size();
    }

    /**
     * Opens a new account with a unique id and the given balance. Safe to call from many threads.
     *
     * <p>Task 3.2. See ROADMAP.md.
     */
    public Account openAccount(long initialBalance) {
        throw new UnsupportedOperationException("Task 3.2: Bank.openAccount is not implemented yet");
    }

    private void init(int accountCount, long initialBalance) {
        for (long i = 0; i < accountCount; i++) {
            accounts.put(i, new Account(i, initialBalance));
        }
    }
}
