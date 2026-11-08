package com.azaika;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;

public class Main {
    private static final int INITIAL_BALANCE = 100_000;
    private static final int TRANSFERS_PER_THREAD = 100_000;
    private static final int MAX_TRANSFER_AMOUNT = 100_000;
    private static final int ACCOUNT_COUNT = 10;
    private static final int THREAD_COUNT = 20;
    private static final LongAdder rejected = new LongAdder();
    private static final LongAdder raceFailure = new LongAdder();
    private static final LongAdder success = new LongAdder();


    public static void main(String[] args) {
        printConfig();

        Instant start = Instant.now();
        Bank bank = new Bank(ACCOUNT_COUNT, INITIAL_BALANCE);
        List<Thread> threads = getThreads(bank);

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        int finalNegativeBalance = 0;
        int inProcessNegativeBalance = 0;
        long minimalBalance = Long.MAX_VALUE;
        for (int i = 0; i < ACCOUNT_COUNT; i++) {
            Account account = bank.findById(i).get();
            if (account.getBalance() < 0) {
                finalNegativeBalance++;
            }
            inProcessNegativeBalance += (int) account.negativeBalanceOccuranceCounter.sum();
            minimalBalance = Math.min(minimalBalance, account.getBalance());
        }

        int expectedSum = INITIAL_BALANCE * ACCOUNT_COUNT;

        Instant end = Instant.now();
        printSummaryReport(expectedSum, bank, minimalBalance, finalNegativeBalance, inProcessNegativeBalance, start, end);
    }

    private static List<Thread> getThreads(Bank bank) {

        NaiveTransferService service = new NaiveTransferService(bank);

        List<Thread> threads = new ArrayList<>(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            Thread thread = new Thread(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int j = 0; j < TRANSFERS_PER_THREAD; j++) {
                    int fromId = random.nextInt(ACCOUNT_COUNT);
                    int targetId = random.nextInt(ACCOUNT_COUNT);
                    int randomAmount = random.nextInt(MAX_TRANSFER_AMOUNT);
                    try {
                        boolean transfer = service.transfer(fromId, targetId, randomAmount);
                        if (transfer) {
                            success.increment();
                        } else {
                            rejected.increment();
                        }
                    } catch (IllegalStateException ignored) {
                        raceFailure.increment();
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }
        return threads;
    }

    private static void printSummaryReport(int exepctedSum, Bank bank, long minimalBalance, int negativeBalance, int inProcessNegativeBalance, Instant start, Instant end) {
        System.out.println("Excpected sum:" + exepctedSum);
        System.out.println("Actual sum:" + bank.totalBalance());
        System.out.println("Difference:" + (bank.totalBalance() - exepctedSum));
        System.out.println("Minimal balance:" + minimalBalance);
        System.out.println("Final Negative balance count:" + negativeBalance);
        System.out.println("In process Negative balance occurs:" + inProcessNegativeBalance);
        System.out.println("Time:" + Duration.between(start, end));
        System.out.println("Rejected:" + rejected.sum());
        System.out.println("Race failure:" + raceFailure.sum());
        System.out.println("Success:" + success.sum());
    }

    private static void printConfig() {
        System.out.println("Cores:" + Runtime.getRuntime().availableProcessors());
        System.out.println("Threads:" + THREAD_COUNT);
        System.out.println("Transfers per thread:" + TRANSFERS_PER_THREAD);
        System.out.println("Total transfers:" + ((long) THREAD_COUNT * TRANSFERS_PER_THREAD));
        System.out.println("Max transfer amount:" + MAX_TRANSFER_AMOUNT);
        System.out.println("Accounts:" + ACCOUNT_COUNT);
        System.out.println("Initial balance:" + INITIAL_BALANCE);
    }

}
