package com.azaika;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AccountTest {
    private static final int INITIAL_BALANCE = 100_000;

    @Test
    void deposit_changesBalance() {
        Account account = new Account(0, INITIAL_BALANCE);
        account.deposit(100);
        assertEquals(INITIAL_BALANCE + 100, account.getBalance());
    }

    @Test
    void deposit_overflow_throws() {
        Account account = new Account(0, INITIAL_BALANCE);

        assertThrows(ArithmeticException.class, () -> account.deposit(Long.MAX_VALUE));
        assertEquals(INITIAL_BALANCE, account.getBalance());
    }

    @Test
    void deposit_negativeAmount_throws() {
        Account account = new Account(0, INITIAL_BALANCE);

        assertThrows(IllegalArgumentException.class, () -> account.deposit(-100));
        assertEquals(INITIAL_BALANCE, account.getBalance());
    }

    @Test
    void deposit_zeroAmount_throws() {
        Account account = new Account(0, INITIAL_BALANCE);

        assertThrows(IllegalArgumentException.class, () -> account.deposit(0));
        assertEquals(INITIAL_BALANCE, account.getBalance());
    }

    @Test
    void withdraw_changesBalance() {
        Account account = new Account(0, INITIAL_BALANCE);
        account.withdraw(100);
        assertEquals(INITIAL_BALANCE - 100, account.getBalance());
    }

    @Test
    void withdraw_allAmount_changesBalance() {
        Account account = new Account(0, INITIAL_BALANCE);
        account.withdraw(INITIAL_BALANCE);
        assertEquals(0, account.getBalance());
    }

    @Test
    void withdraw_negativeAmount_throws() {
        Account account = new Account(0, INITIAL_BALANCE);

        assertThrows(IllegalArgumentException.class, () -> account.withdraw(-100));
        assertEquals(INITIAL_BALANCE, account.getBalance());
    }

    @Test
    void withdraw_zeroAmount_throws() {
        Account account = new Account(0, INITIAL_BALANCE);

        assertThrows(IllegalArgumentException.class, () -> account.withdraw(0));
        assertEquals(INITIAL_BALANCE, account.getBalance());
    }

    @Test
    void withdraw_exceedingBalance_throws() {
        Account account = new Account(0, INITIAL_BALANCE);

        assertThrows(IllegalStateException.class, () -> account.withdraw(INITIAL_BALANCE + 1));
        long balance = account.getBalance();
        assertEquals(INITIAL_BALANCE, balance);
    }

    @Test
    void constructor_validBalance_initializesBalance() {
        Account account = new Account(0, INITIAL_BALANCE);
        assertEquals(INITIAL_BALANCE, account.getBalance());
    }

    @Test
    void constructor_negativeBalance_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Account(0, -1));
    }

}
