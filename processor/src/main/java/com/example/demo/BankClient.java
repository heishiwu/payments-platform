package com.example.demo;

public interface BankClient {
    TxnMessage settle(TxnMessage request);
}

