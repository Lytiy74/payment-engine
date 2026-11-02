package com.azaika;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;

public class Main {
    private final static LongAdder rejected = new LongAdder();
    private final static LongAdder raceFailure = new LongAdder();
    private final static LongAdder success = new LongAdder();


    public static void main(String[] args) {
        Instant start = Instant.now();

        System.out.println("Cores: " + Runtime.getRuntime().availableProcessors());
        int initialBalance = 100_000;
        int accountCount = 10;
        Bank bank = new Bank(accountCount, initialBalance);
        List<Thread> threads = getThreads(bank, accountCount);

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        int negativeBalance = 0;
        long minimalBalance = Long.MAX_VALUE;
        for (int i = 0; i < accountCount; i++) {
            Account account = bank.findById(i).get();
            if (account.getBalance() < 0) {
                negativeBalance++;
            }
            minimalBalance = Math.min(minimalBalance, account.getBalance());
        }

        int exepctedSum = initialBalance * accountCount;
        Instant end = Instant.now();
        System.out.println("Excpected sum: " + exepctedSum);
        System.out.println("Actual sum: " + bank.totalBalance());
        System.out.println("Difference : " + (exepctedSum - bank.totalBalance()));
        System.out.println("Minimal balance: " + minimalBalance);
        System.out.println("Negative balance count: " + negativeBalance);
        System.out.println("Time: " + Duration.between(start, end));
        System.out.println("Rejected: " + rejected.sum());
        System.out.println("Race failure: " + raceFailure.sum());
        System.out.println("Success: " + success.sum());
    }

    private static List<Thread> getThreads(Bank bank, int accountCount) {

        NaiveTransferService service = new NaiveTransferService(bank);

        int threadCount = 20;
        List<Thread> threads = new ArrayList<>(threadCount);
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int j = 0; j < 100_000; j++) {
                    int fromId = random.nextInt(accountCount);
                    int targetId = random.nextInt(accountCount);
                    int randomAmount = random.nextInt(100_000);
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

}
