package com.azaika.payment.transfer;

import com.azaika.payment.account.Account;
import com.azaika.payment.account.Bank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The single-threaded rules every {@link TransferService} must follow.
 * A test class for a concrete strategy extends this class and says how to create the strategy.
 */
abstract class TransferFunctionalContract {
    protected static final int ACCOUNT_COUNT = 10;
    protected static final long INITIAL_BALANCE = 100_000;

    protected Bank bank;
    protected TransferService transferService;

    protected abstract TransferService create(Bank bank);

    @BeforeEach
    void setUp() {
        bank = new Bank(ACCOUNT_COUNT, INITIAL_BALANCE);
        transferService = create(bank);
    }

    protected Account account(long id) {
        return bank.findById(id).orElseThrow();
    }

    @Test
    void transfer_movesMoneyAndKeepsTotal() {
        Account first = account(1);
        Account second = account(2);
        Account third = account(3);

        long amount = 50_000;

        assertEquals(TransferResult.SUCCESS, transferService.transfer(first.getId(), second.getId(), amount));
        assertEquals(INITIAL_BALANCE - amount, first.getBalance());
        assertEquals(INITIAL_BALANCE + amount, second.getBalance());

        assertEquals(TransferResult.SUCCESS, transferService.transfer(second.getId(), third.getId(), amount));
        assertEquals(INITIAL_BALANCE, second.getBalance());
        assertEquals(INITIAL_BALANCE + amount, third.getBalance());

        assertEquals(TransferResult.SUCCESS, transferService.transfer(third.getId(), first.getId(), amount));
        assertEquals(INITIAL_BALANCE, first.getBalance());
        assertEquals(INITIAL_BALANCE, third.getBalance());

        assertEquals(INITIAL_BALANCE * ACCOUNT_COUNT, bank.totalBalance());
    }

    @Test
    void transfer_negativeAmount_returnsInvalidAmount() {
        assertEquals(TransferResult.INVALID_AMOUNT, transferService.transfer(1, 2, -100));
        assertEquals(INITIAL_BALANCE, account(1).getBalance());
        assertEquals(INITIAL_BALANCE, account(2).getBalance());
    }

    @Test
    void transfer_zeroAmount_returnsInvalidAmount() {
        assertEquals(TransferResult.INVALID_AMOUNT, transferService.transfer(1, 2, 0));
        assertEquals(INITIAL_BALANCE, account(1).getBalance());
        assertEquals(INITIAL_BALANCE, account(2).getBalance());
    }

    @Test
    void transfer_exceedingBalance_returnsInsufficientFunds() {
        assertEquals(TransferResult.INSUFFICIENT_FUNDS, transferService.transfer(1, 2, INITIAL_BALANCE + 1));
        assertEquals(INITIAL_BALANCE, account(1).getBalance());
        assertEquals(INITIAL_BALANCE, account(2).getBalance());
    }

    @Test
    void transfer_wholeBalance_succeeds() {
        assertEquals(TransferResult.SUCCESS, transferService.transfer(1, 2, INITIAL_BALANCE));
        assertEquals(0L, account(1).getBalance());
        assertEquals(2 * INITIAL_BALANCE, account(2).getBalance());
    }

    @Test
    void transfer_sameAccount_returnsSameAccount() {
        assertEquals(TransferResult.SAME_ACCOUNT, transferService.transfer(1, 1, 100));
        assertEquals(INITIAL_BALANCE, account(1).getBalance());
    }

    @Test
    void transfer_overflow_returnsBalanceOverflow() {
        Account target = account(2);
        target.deposit(Long.MAX_VALUE - target.getBalance());

        assertEquals(TransferResult.BALANCE_OVERFLOW, transferService.transfer(1, 2, 1));
        assertEquals(INITIAL_BALANCE, account(1).getBalance());
        assertEquals(Long.MAX_VALUE, target.getBalance());
    }

    @Test
    void transfer_upToMaxValue_succeeds() {
        Account target = account(2);
        target.deposit(Long.MAX_VALUE - target.getBalance() - INITIAL_BALANCE);

        assertEquals(TransferResult.SUCCESS, transferService.transfer(1, 2, INITIAL_BALANCE));
        assertEquals(0L, account(1).getBalance());
        assertEquals(Long.MAX_VALUE, target.getBalance());
    }

    @Test
    void transfer_unknownTarget_returnsAccountNotFound() {
        assertEquals(TransferResult.ACCOUNT_NOT_FOUND, transferService.transfer(1, 1000, 1));
        assertEquals(INITIAL_BALANCE, account(1).getBalance());
    }

    @Test
    void transfer_unknownSource_returnsAccountNotFound() {
        assertEquals(TransferResult.ACCOUNT_NOT_FOUND, transferService.transfer(1000, 1, 1));
        assertEquals(INITIAL_BALANCE, account(1).getBalance());
    }
}
