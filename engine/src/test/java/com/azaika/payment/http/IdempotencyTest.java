package com.azaika.payment.http;

import com.azaika.payment.account.Bank;
import com.azaika.payment.processing.PaymentProcessor;
import com.azaika.payment.transfer.TransferResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled("Task 5.2: delete this line when you start the task")
@Timeout(60)
class IdempotencyTest extends HttpTestSupport {

    private HttpResponse<String> postWithKey(String body, String key) throws Exception {
        return client.send(postRequest("/transfers", body, key), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void sameKeyTwice_executesTransferOnce_andRepeatsTheResponse() throws Exception {
        HttpResponse<String> first = postWithKey(transferJson(1, 2, 500), "key-1");
        HttpResponse<String> second = postWithKey(transferJson(1, 2, 500), "key-1");

        assertEquals(200, first.statusCode());
        assertEquals(first.statusCode(), second.statusCode());
        assertEquals(first.body(), second.body());
        assertEquals(INITIAL_BALANCE - 500, balanceOf(1));
        assertEquals(INITIAL_BALANCE + 500, balanceOf(2));
    }

    @Test
    void sameKey_repeatsAFailedResponseToo() throws Exception {
        HttpResponse<String> first = postWithKey(transferJson(1, 2, INITIAL_BALANCE + 1), "key-1");
        HttpResponse<String> second = postWithKey(transferJson(1, 2, INITIAL_BALANCE + 1), "key-1");

        assertEquals(422, first.statusCode());
        assertEquals(422, second.statusCode());
        assertEquals(first.body(), second.body());
    }

    @Test
    void differentKeys_executeTransferEachTime() throws Exception {
        postWithKey(transferJson(1, 2, 500), "key-1");
        postWithKey(transferJson(1, 2, 500), "key-2");

        assertEquals(INITIAL_BALANCE - 1_000, balanceOf(1));
        assertEquals(INITIAL_BALANCE + 1_000, balanceOf(2));
    }

    @Test
    void noKey_executesTransferEachTime() throws Exception {
        postWithKey(transferJson(1, 2, 500), null);
        postWithKey(transferJson(1, 2, 500), null);

        assertEquals(INITIAL_BALANCE - 1_000, balanceOf(1));
        assertEquals(INITIAL_BALANCE + 1_000, balanceOf(2));
    }

    @Test
    void sameKey_fromManyClientsAtOnce_executesTransferExactlyOnce() throws Exception {
        // Replace the default stack with a slow service that counts its calls: while the first request
        // is still being executed, all the others arrive with the same key.
        stopServer();
        AtomicInteger executions = new AtomicInteger();
        processor = new PaymentProcessor((from, to, amount) -> {
            executions.incrementAndGet();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return TransferResult.SUCCESS;
        }, 8, 100);
        server = new PaymentHttpServer(new Bank(ACCOUNT_COUNT, INITIAL_BALANCE), processor, 0);
        server.start();
        int clients = 32;
        List<CompletableFuture<HttpResponse<String>>> responses = new ArrayList<>();

        for (int i = 0; i < clients; i++) {
            responses.add(client.sendAsync(
                    postRequest("/transfers", transferJson(1, 2, 500), "one-and-only"),
                    HttpResponse.BodyHandlers.ofString()));
        }

        for (CompletableFuture<HttpResponse<String>> response : responses) {
            assertEquals(200, response.join().statusCode());
            assertStatusField("SUCCESS", response.join());
        }
        assertEquals(1, executions.get(), "The transfer was executed more than once");
    }
}
