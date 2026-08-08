CREATE EXTENSION IF NOT EXISTS pgcrypto;

DROP TABLE IF EXISTS community_memberships CASCADE;
DROP TABLE IF EXISTS communities CASCADE;

CREATE TABLE communities (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(100) NOT NULL UNIQUE,
    description     VARCHAR(500),
    creator         JSONB        NOT NULL,
    creator_id      UUID         NOT NULL,
    member_count    BIGINT       NOT NULL DEFAULT 0,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- One membership edge per user+community (supports many users, many communities each).
-- Snapshots embed display names so UIs need no extra join.
CREATE TABLE community_memberships (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community       JSONB        NOT NULL,
    community_id    UUID         NOT NULL REFERENCES communities (id) ON DELETE CASCADE,
    member          JSONB        NOT NULL,
    user_id         UUID         NOT NULL,
    joined_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (community_id, user_id)
);

CREATE INDEX idx_memberships_user ON community_memberships (user_id);
CREATE INDEX idx_memberships_community ON community_memberships (community_id);
CREATE INDEX idx_communities_creator ON communities (creator_id);
