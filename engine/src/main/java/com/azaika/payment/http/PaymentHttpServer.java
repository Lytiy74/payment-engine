package com.azaika.payment.http;

import com.azaika.payment.account.Bank;
import com.azaika.payment.processing.PaymentProcessor;

/**
 * The HTTP API of the payment engine, built on the JDK's {@code com.sun.net.httpserver.HttpServer}.
 *
 * <pre>
 * GET  /accounts/{id}                      200 {"id":1,"balance":100000} | 404 | 400 (id is not a number)
 * POST /transfers {"from":1,"to":2,"amount":500}
 *                                          200 {"status":"SUCCESS"}
 *                                          404 {"status":"ACCOUNT_NOT_FOUND"}
 *                                          422 {"status":"INSUFFICIENT_FUNDS"}   also INVALID_AMOUNT, SAME_ACCOUNT, BALANCE_OVERFLOW
 *                                          503 {"status":"QUEUE_FULL"}           also ACCOUNT_BUSY, SHUTTING_DOWN
 *                                          400 body is not valid JSON or a field is missing
 * any other method on a known path         405
 * any other path                           404
 * </pre>
 *
 * <p>Header {@code Idempotency-Key}: requests with the same key execute the transfer once and all
 * get the same response (task 5.2).
 *
 * <p>Tasks 5.1 (endpoints), 5.2 (idempotency), 5.3 (concurrent requests and clean stop). See ROADMAP.md.
 */
public final class PaymentHttpServer implements AutoCloseable {
    private final Bank bank;
    private final PaymentProcessor processor;
    private final int port;

    /**
     * @param port the port to listen on; {@code 0} picks any free port
     */
    public PaymentHttpServer(Bank bank, PaymentProcessor processor, int port) {
        this.bank = bank;
        this.processor = processor;
        this.port = port;
    }

    /** Starts listening. */
    public void start() {
        throw new UnsupportedOperationException("Task 5.1: PaymentHttpServer.start is not implemented yet");
    }

    /** The port the server actually listens on (useful when it was created with port 0). */
    public int port() {
        throw new UnsupportedOperationException("Task 5.1: PaymentHttpServer.port is not implemented yet");
    }

    /** Stops the server. Safe to call more than once. Does not shut down the processor. */
    @Override
    public void close() {
        throw new UnsupportedOperationException("Task 5.3: PaymentHttpServer.close is not implemented yet");
    }
}
