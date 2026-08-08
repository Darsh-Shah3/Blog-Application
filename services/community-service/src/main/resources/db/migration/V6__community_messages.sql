-- Community live chat messages (stored, queryable history)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS community_messages (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id  UUID         NOT NULL REFERENCES communities (id) ON DELETE CASCADE,
    community     JSONB        NOT NULL,
    author_id     UUID         NOT NULL,
    author        JSONB        NOT NULL,
    body          VARCHAR(2000) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_community_messages_community_created
    ON community_messages (community_id, created_at DESC);
