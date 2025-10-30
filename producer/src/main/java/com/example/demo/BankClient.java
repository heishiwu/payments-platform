package com.example.demo;

/** 抽象银行接口：同步扣 / 存款 */
public interface BankClient {
    TxnMessage credit(TxnMessage request);   // 入金
    TxnMessage debit(TxnMessage request);    // 出金
}
