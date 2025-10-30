CREATE TABLE IF NOT EXISTS settlements (
    id SERIAL PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL,
    txn_id VARCHAR(64) NOT NULL,
    batch_id VARCHAR(64),
    txn_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    amount NUMERIC(18, 2) NOT NULL,
    callback_url TEXT NOT NULL,
    event_time TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (merchant_id, txn_id)
);

