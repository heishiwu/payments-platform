package com.example.demo;

import java.time.Instant;
import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * 虚拟银行：10–30 ms 随机延迟，95 % 成功率
 */
@Component                           // 让 Spring 自动装配
public class DummyBankClient implements BankClient {

    private static final Random RND = new Random();

    @Override
    public TxnMessage credit(TxnMessage request) {
        return mockResp(request.withType(TxnType.CREDIT));
    }

    @Override
    public TxnMessage debit(TxnMessage request) {
        return mockResp(request.withType(TxnType.DEBIT));
    }

    /* ---------- 内部辅助 ---------- */
    private TxnMessage mockResp(TxnMessage message) {
        sleep(10, 30);                         // 模拟网络延迟
        //boolean ok = RND.nextDouble() < 0.95;  // 95 % 成功 / 5 % 失败
        boolean ok = true; // 100%成功
        return message.withStatusAndTimestamp(ok ? TxnStatus.SUCCESS : TxnStatus.FAILED, Instant.now());
    }

    private static void sleep(int minMs, int maxMs) {
        try { Thread.sleep(RND.nextInt(maxMs - minMs + 1) + minMs); }
        catch (InterruptedException ignored) {}
    }
}
