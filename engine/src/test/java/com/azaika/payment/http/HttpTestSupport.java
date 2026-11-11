package com.azaika.payment.http;

import com.azaika.payment.account.Bank;
import com.azaika.payment.processing.PaymentProcessor;
import com.azaika.payment.transfer.OrderedLockTransferService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Starts the real stack for HTTP tests: Bank, OrderedLockTransferService (task 2.2),
 * PaymentProcessor (stage 4) and PaymentHttpServer on a free port.
 */
abstract class HttpTestSupport {
    protected static final int ACCOUNT_COUNT = 10;
    protected static final long INITIAL_BALANCE = 100_000;

    protected final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    protected Bank bank;
    protected PaymentProcessor processor;
    protected PaymentHttpServer server;

    @BeforeEach
    void startServer() {
        bank = new Bank(ACCOUNT_COUNT, INITIAL_BALANCE);
        processor = createProcessor(bank);
        server = new PaymentHttpServer(bank, processor, 0);
        server.start();
    }

    @AfterEach
    void stopServer() throws InterruptedException {
        try {
            server.close();
        } catch (UnsupportedOperationException e) {
            // close arrives in task 5.3
        }
        processor.shutdown(Duration.ofSeconds(5));
    }

    protected PaymentProcessor createProcessor(Bank bank) {
        return new PaymentProcessor(new OrderedLockTransferService(bank), 4, 1_000);
    }

    protected URI uri(String path) {
        return URI.create("http://localhost:" + server.port() + path);
    }

    protected HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
        return client.send(postRequest(path, body, null), HttpResponse.BodyHandlers.ofString());
    }

    protected HttpRequest postRequest(String path, String body, String idempotencyKey) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (idempotencyKey != null) {
            builder.header("Idempotency-Key", idempotencyKey);
        }
        return builder.build();
    }

    protected static String transferJson(long from, long to, long amount) {
        return "{\"from\":" + from + ",\"to\":" + to + ",\"amount\":" + amount + "}";
    }

    /** Reads the balance through the API, so it also checks the JSON of GET /accounts/{id}. */
    protected long balanceOf(long id) throws IOException, InterruptedException {
        HttpResponse<String> response = get("/accounts/" + id);
        assertEquals(200, response.statusCode());
        Matcher matcher = Pattern.compile("\"balance\"\\s*:\\s*(-?\\d+)").matcher(response.body());
        assertTrue(matcher.find(), "No \"balance\" field in: " + response.body());
        return Long.parseLong(matcher.group(1));
    }

    protected static void assertStatusField(String expected, HttpResponse<String> response) {
        assertTrue(Pattern.compile("\"status\"\\s*:\\s*\"" + expected + "\"").matcher(response.body()).find(),
                "Expected \"status\":\"" + expected + "\" in the body but got: " + response.body());
    }
}
