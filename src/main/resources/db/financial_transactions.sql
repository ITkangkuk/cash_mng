CREATE TABLE financial_transactions (
    transaction_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    amount NUMERIC(15, 2) NOT NULL,
    transaction_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_financial_transactions_user_date
    ON financial_transactions (user_id, transaction_date DESC, created_at DESC);
