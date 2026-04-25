ALTER TABLE financial_transactions
    ADD COLUMN IF NOT EXISTS payment_completed BOOLEAN NOT NULL DEFAULT FALSE;
