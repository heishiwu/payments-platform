package com.example.demo;

import java.time.Instant;
import java.util.Random;

import org.springframework.stereotype.Component;

@Component
public class DummyBankClient implements BankClient {

    private static final Random RANDOM = new Random();

    @Override
    public TxnMessage settle(TxnMessage request) {
        simulateLatency(15, 45);
        boolean succeeded = RANDOM.nextDouble() < 0.9;
        TxnStatus status = succeeded ? TxnStatus.SUCCESS : TxnStatus.FAILED;
        return request.withStatusAndTimestamp(status, Instant.now());
    }

    private static void simulateLatency(int minMs, int maxMs) {
        try {
            Thread.sleep(RANDOM.nextInt(maxMs - minMs + 1) + minMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

