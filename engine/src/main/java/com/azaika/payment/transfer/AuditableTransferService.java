package com.azaika.payment.transfer;

import com.azaika.payment.account.Bank;

/**
 * Transfers that can be audited while they run: {@link #totalBalance()} never sees money "in flight".
 *
 * <p>Task 3.3. See ROADMAP.md.
 */
public final class AuditableTransferService implements TransferService {
    private final Bank bank;

    public AuditableTransferService(Bank bank) {
        this.bank = bank;
    }

    @Override
    public TransferResult transfer(long fromId, long toId, long amount) {
        throw new UnsupportedOperationException("Task 3.3: AuditableTransferService is not implemented yet");
    }

    /** The sum of all balances as one consistent snapshot, even while transfers are running. */
    public long totalBalance() {
        throw new UnsupportedOperationException("Task 3.3: AuditableTransferService is not implemented yet");
    }
}
