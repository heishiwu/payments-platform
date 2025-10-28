package com.example.demo;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
public class TxnController {

    /* ★ 新增：引入 BankClient，注入 DummyBankClient */
    private final BankClient bank;
    private final KafkaTemplate<String, String> kafka;

    public TxnController(BankClient bank, KafkaTemplate<String, String> kafka) {
        this.bank = bank;
        this.kafka = kafka;
    }

    /* ---------- CREDIT（同步） ---------- */
    @PostMapping(value = "/credit", consumes = "text/plain")
    public BankResp credit(@RequestBody String base64) {
        String payload = decode(base64);
        BankResp resp  = bank.credit(payload);          // ★ 同步调用虚拟银行
        publishEvent(resp);                             // ★ 写入事件流
        return resp;                                    // ★ 返回给客户端
    }

    /* ---------- DEBIT（同步） ---------- */
    @PostMapping(value = "/debit", consumes = "text/plain")
    public BankResp debit(@RequestBody String base64) {
        String payload = decode(base64);
        BankResp resp  = bank.debit(payload);
        publishEvent(resp);
        return resp;
    }

    /* ---------- BATCH（异步） ---------- */
    @PostMapping(value = "/batch", consumes = "text/plain")
    public void batch(@RequestBody String base64) {
        kafka.send(new ProducerRecord<>("batch", base64));
    }

    /* ========== 共用工具 ========== */

    private String decode(String b64) {
        return new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
    }

    private void publishEvent(BankResp r) {             // ★ 统一写事件
        String json = """
            {"txnId":"%s","type":"%s","status":"%s",
             "amount":%s,"currency":"%s","bankTime":"%s"}
            """.formatted(r.txnId(), r.type(), r.status(),
                           r.amount(), r.currency(), r.bankTime());
        kafka.send(new ProducerRecord<>("payment.events", r.txnId(), json));
    }
}
