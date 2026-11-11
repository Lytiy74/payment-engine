package com.azaika.payment.transfer;

import com.azaika.payment.account.Bank;

/** The naive strategy is only expected to be correct in a single thread. */
class NaiveTransferServiceTest extends TransferFunctionalContract {

    @Override
    protected TransferService create(Bank bank) {
        return new NaiveTransferService(bank);
    }
}
