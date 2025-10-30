package com.example.demo;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/transactions")
public class TxnController {

    private static final Logger log = LoggerFactory.getLogger(TxnController.class);

    private final BankClient bank;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;
    private final TxnRepository repository;

    public TxnController(BankClient bank, KafkaTemplate<String, String> kafka, ObjectMapper mapper, TxnRepository repository) {
        this.bank = bank;
        this.kafka = kafka;
        this.mapper = mapper;
        this.repository = repository;
    }

    /* ---------- CREDIT（同步） ---------- */
    @PostMapping(value = "/credit", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public TxnMessage credit(@Valid @RequestBody TxnMessage request) {
        return processWithIdempotency(request, TxnType.CREDIT, bank::credit);
    }

    /* ---------- DEBIT（同步） ---------- */
    @PostMapping(value = "/debit", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public TxnMessage debit(@Valid @RequestBody TxnMessage request) {
        return processWithIdempotency(request, TxnType.DEBIT, bank::debit);
    }

    /* ---------- BATCH CLOSE（异步） ---------- */
    @PostMapping(value = "/batch", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE)
    public TxnMessage batch(@Valid @RequestBody TxnMessage request) {
        TxnMessage normalized = ensureType(request, TxnType.BATCH_CLOSE);
        TxnMessage pending = normalized.status() == TxnStatus.PENDING ? normalized : normalized.withStatus(TxnStatus.PENDING);
        publish(pending);
        return pending;
    }

    /* ========== 共用工具 ========== */

    private TxnMessage ensureType(TxnMessage request, TxnType type) {
        return request.txnType() == type ? request : request.withType(type);
    }

    private TxnMessage processWithIdempotency(TxnMessage request, TxnType forcedType, Function<TxnMessage, TxnMessage> settleFn) {
        TxnMessage normalized = ensureType(request, forcedType);
        return repository.find(normalized.merchantId(), normalized.txnId())
                .map(existing -> {
                    log.info("Transaction {} already settled as {}. Skipping bank call.", existing.txnId(), existing.status());
                    return existing;
                })
                .orElseGet(() -> {
                    TxnMessage settled = settleFn.apply(normalized);
                    repository.upsert(settled);
                    publish(settled);
                    return settled;
                });
    }

    private void publish(TxnMessage message) {
        kafka.send(TxnTopics.forType(message.txnType()), message.idempotencyKey(), serialize(message));
    }

    private String serialize(TxnMessage message) {
        try {
            return mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize transaction %s".formatted(message.txnId()), e);
        }
    }
}
