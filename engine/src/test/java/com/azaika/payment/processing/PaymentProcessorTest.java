package com.azaika.payment.processing;

import com.azaika.payment.testutil.Concurrently;
import com.azaika.payment.transfer.TransferResult;
import com.azaika.payment.transfer.TransferService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Task 4.1: delete this line when you start the task")
class PaymentProcessorTest {
    private static final TransferRequest REQUEST = new TransferRequest(1, 2, 100);

    private PaymentProcessor processor;

    @AfterEach
    void tearDown() throws InterruptedException {
        if (processor == null) {
            return;
        }
        try {
            processor.shutdown(Duration.ofSeconds(5));
        } catch (UnsupportedOperationException e) {
            // shutdown arrives in task 4.3; until then make the worker threads daemons
        }
    }

    @Test
    @Timeout(30)
    void submit_completesFutureWithResultOfTheService() throws Exception {
        processor = new PaymentProcessor(FakeTransferService.returning(TransferResult.INSUFFICIENT_FUNDS), 2, 10);

        CompletableFuture<TransferResult> future = processor.submit(REQUEST);

        assertEquals(TransferResult.INSUFFICIENT_FUNDS, future.get(5, TimeUnit.SECONDS));
    }

    @Test
    @Timeout(30)
    void submit_passesRequestFieldsToTheService() throws Exception {
        Queue<String> seen = new ConcurrentLinkedQueue<>();
        TransferService service = (from, to, amount) -> {
            seen.add(from + ">" + to + ":" + amount);
            return TransferResult.SUCCESS;
        };
        processor = new PaymentProcessor(service, 1, 10);

        processor.submit(new TransferRequest(7, 8, 900)).get(5, TimeUnit.SECONDS);

        assertEquals("7>8:900", seen.peek());
    }

    @Test
    @Timeout(30)
    void submit_executesTransferOnWorkerThread_notOnCallerThread() throws Exception {
        FakeTransferService service = FakeTransferService.returning(TransferResult.SUCCESS);
        processor = new PaymentProcessor(service, 2, 10);

        processor.submit(REQUEST).get(5, TimeUnit.SECONDS);

        assertFalse(service.threads().contains(Thread.currentThread()),
                "The transfer ran on the thread that called submit");
    }

    @Test
    @Timeout(60)
    void submit_fromManyThreads_executesEveryRequestExactlyOnce() {
        int submitters = 4;
        int perSubmitter = 5_000;
        int workers = 4;
        FakeTransferService service = FakeTransferService.returning(TransferResult.SUCCESS);
        processor = new PaymentProcessor(service, workers, submitters * perSubmitter);
        Queue<CompletableFuture<TransferResult>> futures = new ConcurrentLinkedQueue<>();

        Concurrently.run(submitters, Duration.ofSeconds(30), thread -> {
            for (int i = 0; i < perSubmitter; i++) {
                futures.add(processor.submit(REQUEST));
            }
        });
        for (CompletableFuture<TransferResult> future : futures) {
            assertEquals(TransferResult.SUCCESS, future.join());
        }

        assertEquals(submitters * perSubmitter, futures.size());
        assertEquals(submitters * perSubmitter, service.calls());
        assertTrue(service.threads().size() <= workers,
                "Transfers ran on " + service.threads().size() + " threads, but there are only " + workers + " workers");
    }

    @Test
    @Timeout(30)
    void submit_whenServiceThrows_failsThatFutureAndKeepsWorking() throws Exception {
        TransferService service = (from, to, amount) -> {
            if (amount == 13) {
                throw new IllegalStateException("boom");
            }
            return TransferResult.SUCCESS;
        };
        processor = new PaymentProcessor(service, 1, 10);

        CompletableFuture<TransferResult> failed = processor.submit(new TransferRequest(1, 2, 13));
        CompletableFuture<TransferResult> next = processor.submit(REQUEST);

        ExecutionException thrown = assertThrows(ExecutionException.class, () -> failed.get(5, TimeUnit.SECONDS));
        assertEquals("boom", thrown.getCause().getMessage());
        assertEquals(TransferResult.SUCCESS, next.get(5, TimeUnit.SECONDS),
                "The only worker died after an exception in the service");
    }
}
