package com.azaika.payment.http;

import com.azaika.payment.account.Bank;
import com.azaika.payment.processing.PaymentProcessor;
import com.azaika.payment.testutil.Concurrently;
import com.azaika.payment.transfer.TransferResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Task 5.3: delete this line when you start the task")
@Timeout(120)
class PaymentHttpServerConcurrencyTest extends HttpTestSupport {

    @Test
    void manyClients_keepTotalBalance() throws Exception {
        int clients = 16;
        int requestsPerClient = 100;

        Concurrently.run(clients, Duration.ofSeconds(60), thread -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < requestsPerClient; i++) {
                long from = random.nextInt(ACCOUNT_COUNT);
                long to = (from + 1 + random.nextInt(ACCOUNT_COUNT - 1)) % ACCOUNT_COUNT;
                int status = post("/transfers", transferJson(from, to, 1 + random.nextInt(50_000))).statusCode();
                assertTrue(status == 200 || status == 422, "Unexpected HTTP status " + status);
            }
        });

        long total = 0;
        for (long id = 0; id < ACCOUNT_COUNT; id++) {
            total += balanceOf(id);
        }
        assertEquals(INITIAL_BALANCE * ACCOUNT_COUNT, total);
    }

    @Test
    void slowTransfers_areServedInParallel() throws Exception {
        // Replace the default stack: every transfer takes 300 ms and there are enough workers for all of them.
        stopServer();
        int requests = 16;
        processor = new PaymentProcessor((from, to, amount) -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return TransferResult.SUCCESS;
        }, requests, 100);
        server = new PaymentHttpServer(new Bank(ACCOUNT_COUNT, INITIAL_BALANCE), processor, 0);
        server.start();

        long start = System.nanoTime();
        List<CompletableFuture<HttpResponse<String>>> responses = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            responses.add(client.sendAsync(
                    postRequest("/transfers", transferJson(1, 2, 1), null), HttpResponse.BodyHandlers.ofString()));
        }
        for (CompletableFuture<HttpResponse<String>> response : responses) {
            assertEquals(200, response.join().statusCode());
        }
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertTrue(elapsedMillis < 2_500, requests + " requests of 300 ms each took " + elapsedMillis
                + " ms: the server handles them one by one");
    }

    @Test
    void close_stopsAcceptingRequests() throws Exception {
        assertEquals(200, get("/accounts/1").statusCode());
        URI address = uri("/accounts/1");

        server.close();

        assertThrows(IOException.class, () -> client.send(
                HttpRequest.newBuilder(address).timeout(Duration.ofSeconds(3)).GET().build(),
                HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void close_calledTwice_isHarmless() {
        server.close();

        assertDoesNotThrow(() -> server.close());
    }
}
