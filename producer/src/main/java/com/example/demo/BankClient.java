package com.example.demo;

/** 抽象银行接口：同步扣 / 存款 */
public interface BankClient {
    BankResp credit(String rawJson);   // 入金
    BankResp debit(String rawJson);    // 出金
}
