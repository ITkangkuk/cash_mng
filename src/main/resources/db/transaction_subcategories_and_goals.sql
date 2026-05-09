ALTER TABLE financial_transactions
    ADD COLUMN IF NOT EXISTS subcategory VARCHAR(50);

CREATE TABLE IF NOT EXISTS transaction_subcategory_options (
    subcategory_id BIGSERIAL PRIMARY KEY,
    scope_key VARCHAR(100) NOT NULL,
    category_option_id BIGINT NOT NULL,
    label VARCHAR(50) NOT NULL,
    display_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_subcategory_category
        FOREIGN KEY (category_option_id)
        REFERENCES transaction_input_options (option_id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_transaction_subcategory_options_label
    ON transaction_subcategory_options (scope_key, category_option_id, LOWER(label));

CREATE INDEX IF NOT EXISTS idx_transaction_subcategory_options_category_order
    ON transaction_subcategory_options (scope_key, category_option_id, active, display_order, subcategory_id);

CREATE TABLE IF NOT EXISTS spending_goals (
    goal_id BIGSERIAL PRIMARY KEY,
    scope_key VARCHAR(100) NOT NULL,
    category_option_id BIGINT NOT NULL,
    subcategory_option_id BIGINT,
    target_amount NUMERIC(15, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_spending_goals_category
        FOREIGN KEY (category_option_id)
        REFERENCES transaction_input_options (option_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_spending_goals_subcategory
        FOREIGN KEY (subcategory_option_id)
        REFERENCES transaction_subcategory_options (subcategory_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_spending_goals_target_amount
        CHECK (target_amount >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_spending_goals_scope_category
    ON spending_goals (scope_key, category_option_id)
    WHERE subcategory_option_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_spending_goals_scope_subcategory
    ON spending_goals (scope_key, category_option_id, subcategory_option_id)
    WHERE subcategory_option_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_spending_goals_scope
    ON spending_goals (scope_key, category_option_id, subcategory_option_id);
