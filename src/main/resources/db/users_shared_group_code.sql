ALTER TABLE users
    ADD COLUMN IF NOT EXISTS shared_group_code VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_users_shared_group_code
    ON users (shared_group_code);
