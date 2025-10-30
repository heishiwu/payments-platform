package com.example.demo;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@Component
public class TxnListener {

    private static final Logger log = LoggerFactory.getLogger(TxnListener.class);

    private final ObjectMapper mapper;
    private final KafkaTemplate<String, String> kafka;
    private final BankClient bank;
    private final Validator validator;
    private final SettlementRepository settlements;

    public TxnListener(ObjectMapper mapper,
                       KafkaTemplate<String, String> kafka,
                       BankClient bank,
                       Validator validator,
                       SettlementRepository settlements) {
        this.mapper = mapper;
        this.kafka = kafka;
        this.bank = bank;
        this.validator = validator;
        this.settlements = settlements;
    }

    @KafkaListener(topics = TxnTopics.BATCH_CLOSE)
    public void handleBatch(String payload) {
        try {
            TxnMessage message = mapper.readValue(payload, TxnMessage.class);
            validate(message);

            TxnMessage normalized = enforcePending(message);

            settlements.find(normalized.merchantId(), normalized.txnId())
                    .ifPresentOrElse(existing -> {
                        log.info("Batch {} already processed with status {}. Skipping bank call.", existing.txnId(), existing.status());
                        forward(existing);
                    }, () -> {
                        TxnMessage settled = bank.settle(normalized);
                        settlements.upsert(settled);
                        forward(settled);
                    });
        } catch (ConstraintViolationException e) {
            log.warn("Batch message validation failed: {}", e.getMessage());
            publishFailure(payload, e);
        } catch (Exception e) {
            log.error("Failed to process batch message", e);
            publishFailure(payload, e);
        }
    }

    private void forward(TxnMessage message) {
        String topic = message.status() == TxnStatus.SUCCESS ? TxnTopics.CALLBACK : TxnTopics.RETRY;
        String serialized = serialize(message);
        kafka.send(topic, message.idempotencyKey(), serialized);
        log.info("Published {} to topic {}", message.txnId(), topic);
    }

    private void publishFailure(String originalPayload, Exception cause) {
        try {
            TxnMessage fallback = mapper.readValue(originalPayload, TxnMessage.class)
                    .withStatusAndTimestamp(TxnStatus.FAILED, java.time.Instant.now());
            settlements.upsert(fallback);
            kafka.send(TxnTopics.RETRY, fallback.idempotencyKey(), serialize(fallback));
        } catch (Exception deserializationError) {
            log.error("Unable to deserialize payload for retry; message dropped. Original payload: {}", originalPayload, deserializationError);
        }
    }

    private TxnMessage enforcePending(TxnMessage message) {
        TxnMessage adjusted = message;
        if (message.status() != TxnStatus.PENDING) {
            log.debug("Incoming batch {} not marked pending. Coercing to PENDING.", message.txnId());
            adjusted = adjusted.withStatus(TxnStatus.PENDING);
        }
        if (message.txnType() != TxnType.BATCH_CLOSE) {
            log.debug("Incoming batch {} type {} forced to BATCH_CLOSE", message.txnId(), message.txnType());
            adjusted = adjusted.withType(TxnType.BATCH_CLOSE);
        }
        return adjusted;
    }

    private void validate(TxnMessage message) {
        Set<ConstraintViolation<TxnMessage>> violations = validator.validate(message);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private String serialize(TxnMessage message) {
        try {
            return mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize transaction %s".formatted(message.txnId()), e);
        }
    }
}
