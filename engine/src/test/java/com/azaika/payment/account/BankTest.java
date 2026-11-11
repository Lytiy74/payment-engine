package com.azaika.payment.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class BankTest {
    private static final int ACCOUNT_COUNT = 10;
    private static final int INITIAL_BALANCE = 100_000;
    private Bank bank;

    @BeforeEach
    void setUp() {
        bank = new Bank(ACCOUNT_COUNT, INITIAL_BALANCE);
    }

    @Test
    void totalBalance_returnsTotalBalance() {
        assertEquals(INITIAL_BALANCE * ACCOUNT_COUNT, bank.totalBalance());
    }

    @Test
    void findById_knownId_returnsAccount() {
        Optional<Account> account = bank.findById(1);
        assertTrue(account.isPresent());
        assertEquals(INITIAL_BALANCE, account.get().getBalance());
    }

    @Test
    void accountCount_returnsNumberOfAccounts() {
        assertEquals(ACCOUNT_COUNT, bank.accountCount());
    }

    @Test
    void findById_unknownId_returnsEmpty() {
        Optional<Account> account = bank.findById(100);
        assertTrue(account.isEmpty());
    }

}
