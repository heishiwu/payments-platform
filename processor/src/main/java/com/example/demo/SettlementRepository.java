package com.example.demo;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SettlementRepository {

    private static final RowMapper<TxnMessage> ROW_MAPPER = (rs, rowNum) -> new TxnMessage(
            rs.getString("txn_id"),
            rs.getString("batch_id"),
            rs.getString("merchant_id"),
            TxnType.valueOf(rs.getString("txn_type")),
            rs.getBigDecimal("amount"),
            TxnStatus.valueOf(rs.getString("status")),
            rs.getString("callback_url"),
            rs.getTimestamp("event_time").toInstant()
    );

    private static final String SELECT_SQL = """
            SELECT merchant_id, txn_id, batch_id, txn_type, status, amount, callback_url, event_time
            FROM settlements
            WHERE merchant_id = :merchantId AND txn_id = :txnId
            """;

    private static final String UPSERT_SQL = """
            INSERT INTO settlements (
                merchant_id,
                txn_id,
                batch_id,
                txn_type,
                status,
                amount,
                callback_url,
                event_time,
                updated_at
            ) VALUES (
                :merchantId,
                :txnId,
                :batchId,
                :txnType,
                :status,
                :amount,
                :callbackUrl,
                :eventTime,
                :updatedAt
            )
            ON CONFLICT (merchant_id, txn_id)
            DO UPDATE SET
                batch_id     = EXCLUDED.batch_id,
                txn_type     = EXCLUDED.txn_type,
                status       = EXCLUDED.status,
                amount       = EXCLUDED.amount,
                callback_url = EXCLUDED.callback_url,
                event_time   = EXCLUDED.event_time,
                updated_at   = EXCLUDED.updated_at;
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SettlementRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<TxnMessage> find(String merchantId, String txnId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchantId", merchantId)
                .addValue("txnId", txnId);

        List<TxnMessage> results = jdbcTemplate.query(SELECT_SQL, params, ROW_MAPPER);
        return results.stream().findFirst();
    }

    public void upsert(TxnMessage message) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchantId", message.merchantId())
                .addValue("txnId", message.txnId())
                .addValue("batchId", message.batchId())
                .addValue("txnType", message.txnType().name())
                .addValue("status", message.status().name())
                .addValue("amount", message.amount())
                .addValue("callbackUrl", message.callbackUrl())
                .addValue("eventTime", Timestamp.from(message.timestamp()))
                .addValue("updatedAt", Timestamp.from(Instant.now()));

        jdbcTemplate.update(UPSERT_SQL, params);
    }
}

