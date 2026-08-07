-- Soft-delete users + audit usernames.
-- Active-only uniqueness: soft-deleted usernames/emails can be re-registered.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(50),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50),
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

-- Backfill existing rows
UPDATE users
SET created_by = COALESCE(created_by, username),
    updated_by = COALESCE(updated_by, username)
WHERE created_by IS NULL OR updated_by IS NULL;

-- Drop plain unique constraints so partial indexes can take over
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_username_key;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_username_active
    ON users (username) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_active
    ON users (email) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users (deleted_at);
