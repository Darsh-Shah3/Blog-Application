-- Direct messages (1:1 threads, like Discord DMs)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS dm_threads (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_low              UUID         NOT NULL,
    user_high             UUID         NOT NULL,
    user_low_username     VARCHAR(100) NOT NULL DEFAULT 'user',
    user_high_username    VARCHAR(100) NOT NULL DEFAULT 'user',
    last_message_at       TIMESTAMPTZ,
    last_message_preview  VARCHAR(200),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_dm_threads_pair UNIQUE (user_low, user_high),
    CONSTRAINT ck_dm_threads_ordered CHECK (user_low < user_high)
);

CREATE INDEX IF NOT EXISTS idx_dm_threads_user_low_last
    ON dm_threads (user_low, last_message_at DESC NULLS LAST);

CREATE INDEX IF NOT EXISTS idx_dm_threads_user_high_last
    ON dm_threads (user_high, last_message_at DESC NULLS LAST);

CREATE TABLE IF NOT EXISTS dm_messages (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    thread_id   UUID         NOT NULL REFERENCES dm_threads (id) ON DELETE CASCADE,
    author_id   UUID         NOT NULL,
    author      JSONB        NOT NULL,
    body        VARCHAR(2000) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dm_messages_thread_created
    ON dm_messages (thread_id, created_at DESC);
