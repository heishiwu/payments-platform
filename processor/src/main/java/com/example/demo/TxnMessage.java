package com.example.demo;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TxnMessage(
        @NotBlank String txnId,
        String batchId,
        @NotBlank String merchantId,
        @NotNull TxnType txnType,
        @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal amount,
        @NotNull TxnStatus status,
        @NotBlank @Pattern(regexp = "https?://.+", message = "callbackUrl must be http(s)") String callbackUrl,
        @NotNull Instant timestamp
) {

    public TxnMessage withType(TxnType type) {
        return new TxnMessage(txnId, batchId, merchantId, type, amount, status, callbackUrl, timestamp);
    }

    public TxnMessage withStatus(TxnStatus newStatus) {
        return new TxnMessage(txnId, batchId, merchantId, txnType, amount, newStatus, callbackUrl, timestamp);
    }

    public TxnMessage withStatusAndTimestamp(TxnStatus newStatus, Instant newTimestamp) {
        return new TxnMessage(txnId, batchId, merchantId, txnType, amount, newStatus, callbackUrl, newTimestamp);
    }

    public String idempotencyKey() {
        return merchantId + ":" + txnId;
    }
}

