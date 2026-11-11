package com.azaika.payment.processing;

import com.azaika.payment.transfer.TransferResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Task 4.3: delete this line when you start the task")
class PaymentProcessorShutdownTest {
    private static final TransferRequest REQUEST = new TransferRequest(1, 2, 100);

    @Test
    @Timeout(30)
    void shutdown_finishesEverythingAlreadyAccepted() throws Exception {
        FakeTransferService service = FakeTransferService.blocked();
        PaymentProcessor processor = new PaymentProcessor(service, 2, 1_000);
        List<CompletableFuture<TransferResult>> futures = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            futures.add(processor.submit(REQUEST));
        }

        // Nothing is finished yet: two requests are stuck in the workers, 498 wait in the queue.
        service.release();
        boolean finished = processor.shutdown(Duration.ofSeconds(10));

        assertTrue(finished, "shutdown gave up although the work could be finished in time");
        for (CompletableFuture<TransferResult> future : futures) {
            assertTrue(future.isDone(), "An accepted request was dropped during shutdown");
            assertEquals(TransferResult.SUCCESS, future.join());
        }
        assertEquals(500, service.calls());
    }

    @Test
    @Timeout(30)
    void submit_afterShutdown_returnsShuttingDownAndDoesNotReachTheService() throws Exception {
        FakeTransferService service = FakeTransferService.returning(TransferResult.SUCCESS);
        PaymentProcessor processor = new PaymentProcessor(service, 2, 10);
        processor.shutdown(Duration.ofSeconds(5));

        CompletableFuture<TransferResult> future = processor.submit(REQUEST);

        assertTrue(future.isDone(), "A request refused during shutdown must be completed right away");
        assertEquals(TransferResult.SHUTTING_DOWN, future.get(1, TimeUnit.SECONDS));
        assertEquals(0, service.calls());
    }

    @Test
    @Timeout(30)
    void shutdown_whenWorkerIsStuck_returnsFalseAfterTimeout() throws Exception {
        FakeTransferService service = FakeTransferService.blocked();
        PaymentProcessor processor = new PaymentProcessor(service, 1, 10);
        processor.submit(REQUEST);
        assertTrue(service.awaitFirstCall(), "The worker did not pick up the request");

        long start = System.nanoTime();
        boolean finished = processor.shutdown(Duration.ofMillis(300));
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        service.release();

        assertFalse(finished, "shutdown reported success while a request was still running");
        assertTrue(elapsedMillis >= 250, "shutdown returned after " + elapsedMillis + " ms without waiting for the timeout");
        assertTrue(elapsedMillis < 5_000, "shutdown waited " + elapsedMillis + " ms, far longer than the timeout");
    }

    @Test
    @Timeout(30)
    void shutdown_stopsWorkerThreads() throws Exception {
        FakeTransferService service = FakeTransferService.returning(TransferResult.SUCCESS);
        PaymentProcessor processor = new PaymentProcessor(service, 3, 100);
        List<CompletableFuture<TransferResult>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            futures.add(processor.submit(REQUEST));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).get(5, TimeUnit.SECONDS);

        assertTrue(processor.shutdown(Duration.ofSeconds(5)));

        for (Thread worker : service.threads()) {
            worker.join(2_000);
            assertFalse(worker.isAlive(), "Worker thread " + worker.getName() + " is still alive after shutdown");
        }
    }

    @Test
    @Timeout(30)
    void shutdown_calledTwice_isHarmless() throws Exception {
        PaymentProcessor processor = new PaymentProcessor(FakeTransferService.returning(TransferResult.SUCCESS), 2, 10);

        assertTrue(processor.shutdown(Duration.ofSeconds(5)));
        assertTrue(processor.shutdown(Duration.ofSeconds(5)));
    }
}
