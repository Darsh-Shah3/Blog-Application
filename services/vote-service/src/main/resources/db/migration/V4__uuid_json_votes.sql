CREATE EXTENSION IF NOT EXISTS pgcrypto;

DROP TABLE IF EXISTS votes CASCADE;

CREATE TABLE votes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- One row per (user, target); never store multi-user vote rows (unique per user/target).
    voter           JSONB        NOT NULL,
    user_id         UUID         NOT NULL,
    target          JSONB        NOT NULL,
    target_type     VARCHAR(20)  NOT NULL,
    target_id       UUID         NOT NULL,
    value           SMALLINT     NOT NULL CHECK (value IN (-1, 1)),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, target_type, target_id)
);

CREATE INDEX idx_votes_target ON votes (target_type, target_id);
CREATE INDEX idx_votes_user ON votes (user_id);
