package com.azaika;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BankAccountTest {
    private static Bank bank;

    @BeforeAll
    static void setUp() {
        bank = new Bank();
    }

    @Test
    void testDepositAndWithdrawBalanceInOneThread() {
        Account firstAccount = bank.findById(1).get();
        Account secondAccount = bank.findById(2).get();

        firstAccount.deposit(100_000);
        secondAccount.withdraw(100_000);

        long totalBalance = bank.totalBalance();

        assertEquals(1_000_000, totalBalance);
    }

    @Test
    void accountThrowExceptionWhenWithdrawingNegativeAmount() {
        Account account = bank.findById(1).get();
        assertThrows(IllegalArgumentException.class, () -> account.withdraw(-100));
    }

    @Test
    void accountThrowExceptionWhenDepositingNegativeAmount() {
        Account account = bank.findById(1).get();
        assertThrows(IllegalArgumentException.class, () -> account.deposit(-100));
    }

    @Test
    void accountThrowExceptionWhenWithdrawingMoreThanBalance() {
        Account account = bank.findById(1).get();
        assertThrows(IllegalArgumentException.class, () -> account.withdraw(1_000_000));
    }
}
