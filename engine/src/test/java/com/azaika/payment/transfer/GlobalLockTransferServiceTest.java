package com.azaika.payment.transfer;

import com.azaika.payment.account.Bank;

class GlobalLockTransferServiceTest extends TransferConcurrencyContract {

    @Override
    protected TransferService create(Bank bank) {
        return new GlobalLockTransferService(bank);
    }
}
