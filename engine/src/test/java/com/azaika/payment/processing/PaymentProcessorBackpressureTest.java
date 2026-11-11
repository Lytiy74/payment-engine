package com.azaika.payment.processing;

import com.azaika.payment.transfer.TransferResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Task 4.2: delete this line when you start the task")
class PaymentProcessorBackpressureTest {
    private static final TransferRequest REQUEST = new TransferRequest(1, 2, 100);

    private FakeTransferService service;
    private PaymentProcessor processor;

    @BeforeEach
    void setUp() {
        // One worker and room for two waiting requests; the worker is stuck until service.release().
        service = FakeTransferService.blocked();
        processor = new PaymentProcessor(service, 1, 2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        service.release();
        try {
            processor.shutdown(Duration.ofSeconds(5));
        } catch (UnsupportedOperationException e) {
            // shutdown arrives in task 4.3
        }
    }

    @Test
    @Timeout(30)
    void submit_whenQueueIsFull_returnsQueueFullWithoutBlocking() throws Exception {
        CompletableFuture<TransferResult> running = processor.submit(REQUEST);
        assertTrue(service.awaitFirstCall(), "The worker did not pick up the first request");
        CompletableFuture<TransferResult> waiting1 = processor.submit(REQUEST);
        CompletableFuture<TransferResult> waiting2 = processor.submit(REQUEST);

        CompletableFuture<TransferResult> rejected =
                assertTimeoutPreemptively(Duration.ofSeconds(2), () -> processor.submit(REQUEST),
                        "submit blocked on a full queue instead of rejecting the request");

        assertTrue(rejected.isDone(), "A rejected request must be completed right away");
        assertEquals(TransferResult.QUEUE_FULL, rejected.get(1, TimeUnit.SECONDS));
        assertFalse(running.isDone());
        assertFalse(waiting1.isDone());
        assertFalse(waiting2.isDone());
    }

    @Test
    @Timeout(30)
    void acceptedRequests_areExecutedAfterTheWorkerIsFree_rejectedOneIsNot() throws Exception {
        CompletableFuture<TransferResult> running = processor.submit(REQUEST);
        assertTrue(service.awaitFirstCall(), "The worker did not pick up the first request");
        CompletableFuture<TransferResult> waiting1 = processor.submit(REQUEST);
        CompletableFuture<TransferResult> waiting2 = processor.submit(REQUEST);
        processor.submit(REQUEST);

        service.release();

        assertEquals(TransferResult.SUCCESS, running.get(5, TimeUnit.SECONDS));
        assertEquals(TransferResult.SUCCESS, waiting1.get(5, TimeUnit.SECONDS));
        assertEquals(TransferResult.SUCCESS, waiting2.get(5, TimeUnit.SECONDS));
        assertEquals(3, service.calls(), "The rejected request must never reach the service");
    }

    @Test
    @Timeout(30)
    void submit_afterQueueHasRoomAgain_isAccepted() throws Exception {
        CompletableFuture<TransferResult> running = processor.submit(REQUEST);
        assertTrue(service.awaitFirstCall(), "The worker did not pick up the first request");
        CompletableFuture<TransferResult> waiting1 = processor.submit(REQUEST);
        CompletableFuture<TransferResult> waiting2 = processor.submit(REQUEST);
        assertEquals(TransferResult.QUEUE_FULL, processor.submit(REQUEST).get(1, TimeUnit.SECONDS));

        service.release();
        CompletableFuture.allOf(running, waiting1, waiting2).get(5, TimeUnit.SECONDS);

        assertEquals(TransferResult.SUCCESS, processor.submit(REQUEST).get(5, TimeUnit.SECONDS));
    }
}
