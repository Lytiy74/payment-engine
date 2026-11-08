package com.azaika;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TransferServiceTest {
    private static final int ACCOUNT_COUNT = 10;
    private static final int INITIAL_BALANCE = 100_000;

    private Bank bank;
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        bank = new Bank(ACCOUNT_COUNT, INITIAL_BALANCE);
        transferService = new NaiveTransferService(bank);
    }

    @Test
    void verifyTotalBalancePostTransfers() {
        Account firstAccount = bank.findById(1).get();
        Account secondAccount = bank.findById(2).get();
        Account thirdAccount = bank.findById(3).get();

        int transferAmount = 50_000;

        assertTrue(transferService.transfer(firstAccount.getId(), secondAccount.getId(), transferAmount));
        assertEquals(INITIAL_BALANCE - transferAmount, firstAccount.getBalance());
        assertEquals(INITIAL_BALANCE + transferAmount, secondAccount.getBalance());

        assertTrue(transferService.transfer(secondAccount.getId(), thirdAccount.getId(), transferAmount));
        assertEquals(INITIAL_BALANCE, secondAccount.getBalance());
        assertEquals(INITIAL_BALANCE + transferAmount, thirdAccount.getBalance());

        assertTrue(transferService.transfer(thirdAccount.getId(), firstAccount.getId(), transferAmount));
        assertEquals(INITIAL_BALANCE, firstAccount.getBalance());
        assertEquals(INITIAL_BALANCE, thirdAccount.getBalance());

        long totalBalance = bank.totalBalance();

        assertEquals((long) INITIAL_BALANCE * ACCOUNT_COUNT, totalBalance);
    }

    @Test
    void transfer_negativeAmount_returnsFalse() {
        Account firstAccount = bank.findById(1).get();
        Account secondAccount = bank.findById(2).get();

        assertFalse(transferService.transfer(firstAccount.getId(), secondAccount.getId(), -100));
        assertEquals(INITIAL_BALANCE, firstAccount.getBalance());
        assertEquals(INITIAL_BALANCE, secondAccount.getBalance());
    }

    @Test
    void transfer_zeroAmount_returnsFalse() {
        Account firstAccount = bank.findById(1).get();
        Account secondAccount = bank.findById(2).get();

        assertFalse(transferService.transfer(firstAccount.getId(), secondAccount.getId(), 0));
        assertEquals(INITIAL_BALANCE, firstAccount.getBalance());
        assertEquals(INITIAL_BALANCE, secondAccount.getBalance());
    }

    @Test
    void transfer_exceedingBalance_returnsFalse() {
        Account firstAccount = bank.findById(1).get();
        Account secondAccount = bank.findById(2).get();

        assertFalse(
                transferService.transfer(
                        firstAccount.getId(), secondAccount.getId(), firstAccount.getBalance() + 1
                )
        );
        assertEquals(INITIAL_BALANCE, firstAccount.getBalance());
        assertEquals(INITIAL_BALANCE, secondAccount.getBalance());
    }

    @Test
    void transfer_selfAccount_returnsFalse() {
        Account account = bank.findById(1).get();

        assertFalse(transferService.transfer(account.getId(), account.getId(), 100));
        assertEquals(INITIAL_BALANCE, account.getBalance());
    }

    @Test
    void transfer_overflow_returnsFalse() {
        Account source = bank.findById(1).get();
        Account target = bank.findById(2).get();

        target.deposit(Long.MAX_VALUE - target.getBalance());

        assertFalse(transferService.transfer(source.getId(), target.getId(), 1));
        assertEquals(INITIAL_BALANCE, source.getBalance());
        assertEquals(Long.MAX_VALUE, target.getBalance());
    }

    @Test
    void transfer_maxValue_returnsTrue() {
        Account source = bank.findById(1).get();
        Account target = bank.findById(2).get();

        target.deposit(Long.MAX_VALUE - target.getBalance() - INITIAL_BALANCE);

        assertTrue(transferService.transfer(source.getId(), target.getId(), INITIAL_BALANCE));
        assertEquals(0, source.getBalance());
        assertEquals(Long.MAX_VALUE, target.getBalance());
    }

    @Test
    void transfer_targetUnknownAccount_returnsFalse() {
        Account source = bank.findById(1).get();

        assertFalse(transferService.transfer(source.getId(), 1000, 1));
        assertEquals(INITIAL_BALANCE, source.getBalance());
    }

    @Test
    void transfer_sourceUnknownAccount_returnsFalse() {
        Account target = bank.findById(1).get();

        assertFalse(transferService.transfer(1000, target.getId(), 1));
        assertEquals(INITIAL_BALANCE, target.getBalance());
    }

}
