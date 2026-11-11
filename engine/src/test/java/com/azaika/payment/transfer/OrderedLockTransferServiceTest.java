package com.azaika.payment.transfer;

import com.azaika.payment.account.Bank;
import org.junit.jupiter.api.Disabled;

@Disabled("Task 2.2: delete this line when you start the task")
class OrderedLockTransferServiceTest extends TransferConcurrencyContract {

    @Override
    protected TransferService create(Bank bank) {
        return new OrderedLockTransferService(bank);
    }
}
