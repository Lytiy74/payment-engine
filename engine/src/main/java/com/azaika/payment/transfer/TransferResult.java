package com.azaika.payment.transfer;

/** The outcome of a transfer. Only {@link #SUCCESS} changes balances. */
public enum TransferResult {
    SUCCESS,
    /** The amount is zero or negative. */
    INVALID_AMOUNT,
    /** Source and target are the same account. */
    SAME_ACCOUNT,
    /** Source or target account does not exist. */
    ACCOUNT_NOT_FOUND,
    /** The source balance is smaller than the amount. */
    INSUFFICIENT_FUNDS,
    /** The target balance would exceed {@link Long#MAX_VALUE}. */
    BALANCE_OVERFLOW,
    /** An account lock was not acquired in time (from task 2.3). */
    ACCOUNT_BUSY,
    /** The processing queue is full (from task 4.2). */
    QUEUE_FULL,
    /** The processor no longer accepts requests (from task 4.3). */
    SHUTTING_DOWN;

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
