package com.azaika.payment.processing;

/** A transfer waiting to be processed. */
public record TransferRequest(long fromId, long toId, long amount) {
}
