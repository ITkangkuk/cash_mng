CREATE TABLE IF NOT EXISTS transaction_input_options (
    option_id BIGSERIAL PRIMARY KEY,
    scope_key VARCHAR(100) NOT NULL,
    option_kind VARCHAR(20) NOT NULL,
    label VARCHAR(50) NOT NULL,
    type_value VARCHAR(20),
    display_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_transaction_input_options_kind
        CHECK (option_kind IN ('CATEGORY', 'TYPE')),
    CONSTRAINT chk_transaction_input_options_type_value
        CHECK (
            (option_kind = 'CATEGORY' AND type_value IS NULL)
            OR (option_kind = 'TYPE' AND type_value IN ('INCOME', 'EXPENSE'))
        )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_transaction_input_options_label
    ON transaction_input_options (scope_key, option_kind, LOWER(label));

CREATE INDEX IF NOT EXISTS idx_transaction_input_options_scope_kind_order
    ON transaction_input_options (scope_key, option_kind, active, display_order, option_id);

WITH scopes AS (
    SELECT DISTINCT
        CASE
            WHEN shared_group_code IS NOT NULL AND BTRIM(shared_group_code) <> ''
                THEN 'GROUP:' || shared_group_code
            ELSE 'USER:' || user_id
        END AS scope_key
    FROM users
),
default_options AS (
    SELECT 'CATEGORY' AS option_kind, label, NULL::VARCHAR(20) AS type_value, display_order
    FROM (VALUES
        ('식비', 1),
        ('월급', 2),
        ('교통비', 3),
        ('주거비', 4),
        ('통신비', 5),
        ('쇼핑', 6)
    ) AS category_defaults(label, display_order)
    UNION ALL
    SELECT 'TYPE' AS option_kind, label, type_value, display_order
    FROM (VALUES
        ('지출', 'EXPENSE', 1),
        ('수입', 'INCOME', 2)
    ) AS type_defaults(label, type_value, display_order)
)
INSERT INTO transaction_input_options (
    scope_key,
    option_kind,
    label,
    type_value,
    display_order,
    active,
    created_at,
    updated_at
)
SELECT
    scopes.scope_key,
    default_options.option_kind,
    default_options.label,
    default_options.type_value,
    default_options.display_order,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM scopes
CROSS JOIN default_options
ON CONFLICT DO NOTHING;
