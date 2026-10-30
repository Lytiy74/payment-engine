package com.azaika;

import java.util.Optional;

public class NaiveTransferService implements TransferService {
    private final Bank bank;

    public NaiveTransferService(Bank bank) {
        this.bank = bank;
    }

    @Override
    public boolean transfer(long fromId, long toId, long amount) {
        if (amount <= 0) {
            return false;
        }

        Optional<Account> source = bank.findById(fromId);
        if (source.isEmpty()) {
            return false;
        }

        Optional<Account> target = bank.findById(toId);
        if (target.isEmpty()) {
            return false;
        }

        if (source.get().getBalance() < amount) {
            return false;
        }

        source.get().withdraw(amount);
        target.get().deposit(amount);

        return true;
    }

}
