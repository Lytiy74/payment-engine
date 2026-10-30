package com.azaika;

public interface TransferService {
    boolean transfer(long fromId, long toId, long amount);
}
