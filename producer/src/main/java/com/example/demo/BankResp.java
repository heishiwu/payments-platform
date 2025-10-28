package com.example.demo;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 假银行返回值：同步交易的结果
 */
public record BankResp(
        String txnId,          // 交易流水号
        String type,           // CREDIT / DEBIT
        String status,         // SUCCEEDED / FAILED
        BigDecimal amount,     // 金额
        String currency,       // 币种
        Instant bankTime       // 银行确认时间
) {}
