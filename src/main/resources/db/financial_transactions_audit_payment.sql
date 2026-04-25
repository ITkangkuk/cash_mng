ALTER TABLE financial_transactions
    ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS payment_required BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE financial_transactions ft
SET created_by_name = COALESCE(NULLIF(users.user_name, ''), '알 수 없음')
FROM users
WHERE ft.user_id = users.user_id
  AND ft.created_by_name IS NULL;

UPDATE financial_transactions
SET created_by_name = '알 수 없음'
WHERE created_by_name IS NULL;

CREATE INDEX IF NOT EXISTS idx_financial_transactions_payment_required
    ON financial_transactions (payment_required);
