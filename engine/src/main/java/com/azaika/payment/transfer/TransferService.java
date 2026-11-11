package com.azaika.payment.transfer;

/**
 * Moves money between two accounts.
 *
 * <p>A transfer either happens completely or not at all, and it never throws for a business reason:
 * the outcome is reported as a {@link TransferResult}.
 */
public interface TransferService {
    TransferResult transfer(long fromId, long toId, long amount);
}
