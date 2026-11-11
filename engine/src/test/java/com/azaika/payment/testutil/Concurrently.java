package com.azaika.payment.testutil;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Runs the same task in several threads that all start at the same moment. */
public final class Concurrently {

    @FunctionalInterface
    public interface Task {
        void run(int threadIndex) throws Exception;
    }

    private Concurrently() {
    }

    /**
     * Starts {@code threads} threads, releases them together and waits until all of them finish.
     *
     * @throws AssertionError if a thread throws, or if the threads are still running after {@code timeout}
     *                        (the message then says whether the JVM sees a deadlock)
     */
    public static void run(int threads, Duration timeout, Task task) {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();
        List<Thread> workers = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            int index = i;
            Thread worker = new Thread(() -> {
                try {
                    start.await();
                    task.run(index);
                } catch (Throwable e) {
                    failures.add(e);
                } finally {
                    done.countDown();
                }
            }, "test-worker-" + i);
            worker.setDaemon(true);
            workers.add(worker);
            worker.start();
        }
        start.countDown();

        boolean finished;
        try {
            finished = done.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for the worker threads", e);
        }
        if (!finished) {
            throw new AssertionError(describeStuck(workers, timeout));
        }
        if (!failures.isEmpty()) {
            Throwable first = failures.peek();
            throw new AssertionError(failures.size() + " worker thread(s) threw an exception, the first one: " + first, first);
        }
    }

    private static String describeStuck(List<Thread> workers, Duration timeout) {
        StringBuilder message = new StringBuilder();
        long[] deadlocked = ManagementFactory.getThreadMXBean().findDeadlockedThreads();
        if (deadlocked != null) {
            message.append("DEADLOCK: the JVM reports ").append(deadlocked.length).append(" deadlocked threads. ");
        }
        message.append("Worker threads did not finish in ").append(timeout.toSeconds()).append(" s.");
        for (Thread worker : workers) {
            if (!worker.isAlive()) {
                continue;
            }
            message.append(System.lineSeparator()).append("  ").append(worker.getName())
                    .append(" is ").append(worker.getState());
            StackTraceElement[] stack = worker.getStackTrace();
            for (int i = 0; i < Math.min(4, stack.length); i++) {
                message.append(System.lineSeparator()).append("      at ").append(stack[i]);
            }
        }
        return message.toString();
    }
}
