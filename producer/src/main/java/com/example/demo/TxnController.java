package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
public class TxnController {

    private final BankClient bank;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    public TxnController(BankClient bank, KafkaTemplate<String, String> kafka, ObjectMapper mapper) {
        this.bank = bank;
        this.kafka = kafka;
        this.mapper = mapper;
    }

    /* ---------- CREDIT（同步） ---------- */
    @PostMapping(value = "/credit", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public TxnMessage credit(@Valid @RequestBody TxnMessage request) {
        TxnMessage normalized = ensureType(request, TxnType.CREDIT);
        TxnMessage settled = bank.credit(normalized);
        publish(settled);
        return settled;
    }

    /* ---------- DEBIT（同步） ---------- */
    @PostMapping(value = "/debit", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public TxnMessage debit(@Valid @RequestBody TxnMessage request) {
        TxnMessage normalized = ensureType(request, TxnType.DEBIT);
        TxnMessage settled = bank.debit(normalized);
        publish(settled);
        return settled;
    }

    /* ---------- BATCH CLOSE（异步） ---------- */
    @PostMapping(value = "/batch", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public TxnMessage batch(@Valid @RequestBody TxnMessage request) {
        TxnMessage normalized = ensureType(request, TxnType.BATCH_CLOSE);
        TxnMessage pending = normalized.status() == TxnStatus.PENDING ? normalized : normalized.withStatus(TxnStatus.PENDING);
        publish(pending);
        return pending;
    }

    /* ========== 共用工具 ========== */

    private void publish(TxnMessage message) {
        kafka.send(TxnTopics.forType(message.txnType()), message.idempotencyKey(), serialize(message));
    }

    private TxnMessage ensureType(TxnMessage request, TxnType type) {
        return request.txnType() == type ? request : request.withType(type);
    }

    private String serialize(TxnMessage message) {
        try {
            return mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize transaction %s".formatted(message.txnId()), e);
        }
    }
}
