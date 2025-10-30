package com.example.demo;

/**
 * Centralized topic name definitions used by the producer.
 */
public final class TxnTopics {

    private TxnTopics() {
    }

    public static final String CREDIT = "credit";
    public static final String DEBIT = "debit";
    public static final String BATCH_CLOSE = "batch.close";

    public static String forType(TxnType type) {
        return switch (type) {
            case CREDIT -> CREDIT;
            case DEBIT -> DEBIT;
            case BATCH_CLOSE -> BATCH_CLOSE;
        };
    }
}

