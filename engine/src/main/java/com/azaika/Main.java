package com.azaika;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    private static AtomicInteger err = new AtomicInteger(0);
    private static AtomicInteger success = new AtomicInteger(0);


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

        int exepctedSum = initialBalance * accountCount;
        System.out.println("Excpected sum: " + exepctedSum);
        System.out.println("Actual sum: " + bank.totalBalance());
        System.out.println("Difference : " + (exepctedSum - bank.totalBalance()));
        Instant end = Instant.now();
        System.out.println("Time: " + Duration.between(start, end));
        System.out.println("Err: " + err.get());
        System.out.println("Success: " + success.get());
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
                        if (!transfer) {
                            err.incrementAndGet();
                        } else {
                            success.incrementAndGet();
                        }
                    } catch (IllegalStateException ignored) {
                        err.incrementAndGet();
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }
        return threads;
    }

}
