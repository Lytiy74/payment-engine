package com.azaika.payment.transfer;

import com.azaika.payment.account.Bank;
import org.junit.jupiter.api.Disabled;

class OrderedLockTransferServiceTest extends TransferConcurrencyContract {

    @Override
    protected TransferService create(Bank bank) {
        return new OrderedLockTransferService(bank);
    }
}
