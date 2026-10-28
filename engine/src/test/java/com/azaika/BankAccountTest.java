package com.azaika;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class BankAccountTest {
    private static final int ACCOUNT_COUNT = 10;
    private static final int INITIAL_BALANCE = 1_000_000;
    private Bank bank;

    @BeforeEach
    void setUp() {
        bank = new Bank(ACCOUNT_COUNT, INITIAL_BALANCE);
    }

    @Test
    void verifyTotalBalancePostTransfers() {
        Account firstAccount = bank.findById(1).get();
        Account secondAccount = bank.findById(2).get();
        Account thirdAccount = bank.findById(3).get();

        firstAccount.transfer(100_000, secondAccount);
        secondAccount.transfer(100_000, thirdAccount);
        thirdAccount.transfer(100_000, firstAccount);

        long totalBalance = bank.totalBalance();

        assertEquals((long) INITIAL_BALANCE * ACCOUNT_COUNT, totalBalance);
        assertEquals(INITIAL_BALANCE, firstAccount.getBalance());
        assertEquals(INITIAL_BALANCE, secondAccount.getBalance());
        assertEquals(INITIAL_BALANCE, thirdAccount.getBalance());
    }

    @Test
    void withdraw_changesBalance() {
        Account account = bank.findById(1).get();
        account.withdraw(100);
        assertEquals(INITIAL_BALANCE - 100, account.getBalance());
    }

    @Test
    void deposit_changesBalance() {
        Account account = bank.findById(1).get();
        account.deposit(100);
        assertEquals(INITIAL_BALANCE + 100, account.getBalance());
    }

    @Test
    void deposit_overflow_throws() {
        Account account = bank.findById(1).get();

        assertThrows(ArithmeticException.class, () -> account.deposit(Long.MAX_VALUE));
        assertEquals(INITIAL_BALANCE, account.getBalance());
    }

    @Test
    void transfer_negativeAmount_throws() {
        Account firstAccount = bank.findById(1).get();
        Account secondAccount = bank.findById(2).get();

        assertThrows(IllegalArgumentException.class, () -> firstAccount.transfer(-100, secondAccount));
        assertEquals(INITIAL_BALANCE, firstAccount.getBalance());
        assertEquals(INITIAL_BALANCE, secondAccount.getBalance());
    }

    @Test
    void transfer_zeroAmount_throws() {
        Account firstAccount = bank.findById(1).get();
        Account secondAccount = bank.findById(2).get();

        assertThrows(IllegalArgumentException.class, () -> firstAccount.transfer(0, secondAccount));
        assertEquals(INITIAL_BALANCE, firstAccount.getBalance());
        assertEquals(INITIAL_BALANCE, secondAccount.getBalance());
    }

    @Test
    void transfer_exceedingBalance_throws() {
        Account firstAccount = bank.findById(1).get();
        Account secondAccount = bank.findById(2).get();

        assertThrows(IllegalStateException.class, () -> firstAccount.transfer(INITIAL_BALANCE + 1, secondAccount));
        assertEquals(INITIAL_BALANCE, firstAccount.getBalance());
        assertEquals(INITIAL_BALANCE, secondAccount.getBalance());
    }

    @Test
    void transfer_nullAccount_throws() {
        Account firstAccount = bank.findById(1).get();
        Account secondAccount = bank.findById(2).get();

        assertThrows(IllegalArgumentException.class, () -> firstAccount.transfer(100, null));
        assertEquals(INITIAL_BALANCE, firstAccount.getBalance());
        assertEquals(INITIAL_BALANCE, secondAccount.getBalance());
    }

    @Test
    void transfer_selfAccount_throws() {
        Account account = bank.findById(1).get();

        assertThrows(IllegalArgumentException.class, () -> account.transfer(100, account));
        assertEquals(INITIAL_BALANCE, account.getBalance());
    }

    @Test void transfer_overflow_throws() {
        Account source = new Account(1, 100);
        Account target = new Account(2, Long.MAX_VALUE);

        assertThrows(ArithmeticException.class, () -> source.transfer(1, target));
        assertEquals(100, source.getBalance());
        assertEquals(Long.MAX_VALUE, target.getBalance());
    }

    @Test
    void withdraw_negativeAmount_throws() {
        Account account = bank.findById(1).get();

        assertThrows(IllegalArgumentException.class, () -> account.withdraw(-100));
        assertEquals(INITIAL_BALANCE, account.getBalance());
    }

    @Test
    void withdraw_zeroAmount_throws() {
        Account account = bank.findById(1).get();

        assertThrows(IllegalArgumentException.class, () -> account.withdraw(0));
        assertEquals(INITIAL_BALANCE, account.getBalance());
    }

    @Test
    void deposit_negativeAmount_throws() {
        Account account = bank.findById(1).get();

        assertThrows(IllegalArgumentException.class, () -> account.deposit(-100));
        assertEquals(INITIAL_BALANCE, account.getBalance());
    }

    @Test
    void deposit_zeroAmount_throws() {
        Account account = bank.findById(1).get();

        assertThrows(IllegalArgumentException.class, () -> account.deposit(0));
        assertEquals(INITIAL_BALANCE, account.getBalance());
    }

    @Test
    void withdraw_exceedingBalance_throws() {
        Account account = bank.findById(1).get();
        long balance = account.getBalance();
        assertThrows(IllegalStateException.class, () -> account.withdraw(balance + 1));
        assertEquals(INITIAL_BALANCE, balance);
    }

    @Test
    void findById_unknownId_returnsEmpty() {
        Optional<Account> account = bank.findById(100);
        assertTrue(account.isEmpty());
    }

}
