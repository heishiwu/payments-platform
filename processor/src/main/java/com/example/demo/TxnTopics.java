package com.example.demo;

public final class TxnTopics {

    private TxnTopics() {
    }

    public static final String CREDIT = "credit";
    public static final String DEBIT = "debit";
    public static final String BATCH_CLOSE = "batch.close";
    public static final String RETRY = "retry";
    public static final String CALLBACK = "callback";

    public static String forType(TxnType type) {
        return switch (type) {
            case CREDIT -> CREDIT;
            case DEBIT -> DEBIT;
            case BATCH_CLOSE -> BATCH_CLOSE;
        };
    }
}

