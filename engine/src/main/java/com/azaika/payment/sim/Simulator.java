package com.azaika.payment.sim;

import com.azaika.payment.account.Account;
import com.azaika.payment.account.Bank;
import com.azaika.payment.transfer.AuditableTransferService;
import com.azaika.payment.transfer.GlobalLockTransferService;
import com.azaika.payment.transfer.NaiveTransferService;
import com.azaika.payment.transfer.OrderedLockTransferService;
import com.azaika.payment.transfer.TransferResult;
import com.azaika.payment.transfer.TransferService;
import com.azaika.payment.transfer.TryLockTransferService;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * Load simulator: N threads make random transfers, then the report shows whether money was lost.
 *
 * <pre>
 * java -jar engine/target/engine.jar --strategy=naive
 * java -jar engine/target/engine.jar --strategy=ordered --threads=20 --transfers=100000 --accounts=10
 * </pre>
 *
 * <p>Strategies: naive, global, ordered, trylock, auditable.
 */
public final class Simulator {
    private static final Map<TransferResult, LongAdder> results = new EnumMap<>(TransferResult.class);
    private static final LongAdder raceFailures = new LongAdder();
    private static final AtomicReference<UnsupportedOperationException> notImplemented = new AtomicReference<>();

    private static String strategy = "naive";
    private static int threadCount = 20;
    private static int transfersPerThread = 100_000;
    private static int accountCount = 10;
    private static long initialBalance = 100_000;
    private static int maxTransferAmount = 100_000;

    private Simulator() {
    }

    public static void main(String[] args) {
        parseArgs(args);
        for (TransferResult result : TransferResult.values()) {
            results.put(result, new LongAdder());
        }
        printConfig();

        Bank bank = new Bank(accountCount, initialBalance);
        TransferService service = createService(strategy, bank);

        Instant start = Instant.now();
        List<Thread> threads = startThreads(service);
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        Instant end = Instant.now();

        if (notImplemented.get() != null) {
            System.out.println("Strategy '" + strategy + "' is not ready: " + notImplemented.get().getMessage());
            System.exit(2);
        }
        printReport(bank, Duration.between(start, end));
    }

    private static TransferService createService(String name, Bank bank) {
        return switch (name) {
            case "naive" -> new NaiveTransferService(bank);
            case "global" -> new GlobalLockTransferService(bank);
            case "ordered" -> new OrderedLockTransferService(bank);
            case "trylock" -> new TryLockTransferService(bank, Duration.ofMillis(50));
            case "auditable" -> new AuditableTransferService(bank);
            default -> throw new IllegalArgumentException(
                    "Unknown strategy '" + name + "'. Use: naive, global, ordered, trylock, auditable");
        };
    }

    private static List<Thread> startThreads(TransferService service) {
        List<Thread> threads = new ArrayList<>(threadCount);
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int j = 0; j < transfersPerThread; j++) {
                    int fromId = random.nextInt(accountCount);
                    int targetId = random.nextInt(accountCount);
                    int amount = random.nextInt(maxTransferAmount);
                    try {
                        results.get(service.transfer(fromId, targetId, amount)).increment();
                    } catch (UnsupportedOperationException e) {
                        notImplemented.compareAndSet(null, e);
                        return;
                    } catch (IllegalStateException e) {
                        // Account.withdraw refused: the balance changed between the check and the withdrawal.
                        raceFailures.increment();
                    }
                }
            }, "sim-" + i);
            threads.add(thread);
            thread.start();
        }
        return threads;
    }

    private static void printReport(Bank bank, Duration time) {
        long expectedSum = initialBalance * accountCount;
        long actualSum = bank.totalBalance();
        long minimalBalance = Long.MAX_VALUE;
        int negativeBalances = 0;
        for (long id = 0; id < accountCount; id++) {
            Account account = bank.findById(id).orElseThrow();
            minimalBalance = Math.min(minimalBalance, account.getBalance());
            if (account.getBalance() < 0) {
                negativeBalances++;
            }
        }

        System.out.println();
        System.out.println("Expected sum:      " + expectedSum);
        System.out.println("Actual sum:        " + actualSum);
        System.out.println("Difference:        " + (actualSum - expectedSum));
        System.out.println("Minimal balance:   " + minimalBalance);
        System.out.println("Negative balances: " + negativeBalances);
        System.out.println("Race failures:     " + raceFailures.sum());
        System.out.println("Time:              " + time.toMillis() + " ms");
        System.out.println();
        for (TransferResult result : TransferResult.values()) {
            long count = results.get(result).sum();
            if (count > 0) {
                System.out.println(result + ": " + count);
            }
        }
        System.out.println();
        boolean ok = actualSum == expectedSum && negativeBalances == 0 && raceFailures.sum() == 0;
        System.out.println(ok ? "RESULT: OK, no money lost" : "RESULT: BROKEN, the invariant does not hold");
    }

    private static void printConfig() {
        System.out.println("Strategy:             " + strategy);
        System.out.println("Cores:                " + Runtime.getRuntime().availableProcessors());
        System.out.println("Threads:              " + threadCount);
        System.out.println("Transfers per thread: " + transfersPerThread);
        System.out.println("Total transfers:      " + ((long) threadCount * transfersPerThread));
        System.out.println("Max transfer amount:  " + maxTransferAmount);
        System.out.println("Accounts:             " + accountCount);
        System.out.println("Initial balance:      " + initialBalance);
    }

    private static void parseArgs(String[] args) {
        for (String arg : args) {
            String[] parts = arg.split("=", 2);
            if (parts.length != 2 || !parts[0].startsWith("--")) {
                throw new IllegalArgumentException("Expected --name=value but got: " + arg);
            }
            String value = parts[1];
            switch (parts[0]) {
                case "--strategy" -> strategy = value;
                case "--threads" -> threadCount = Integer.parseInt(value);
                case "--transfers" -> transfersPerThread = Integer.parseInt(value);
                case "--accounts" -> accountCount = Integer.parseInt(value);
                case "--balance" -> initialBalance = Long.parseLong(value);
                case "--max-amount" -> maxTransferAmount = Integer.parseInt(value);
                default -> throw new IllegalArgumentException("Unknown option: " + parts[0]);
            }
        }
    }
}
