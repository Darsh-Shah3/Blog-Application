-- Rebuild posts: UUID PK, denormalized community/author JSON, multi-link + multi-media.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

DROP TABLE IF EXISTS posts CASCADE;

CREATE TABLE posts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community       JSONB        NOT NULL,
    community_id    UUID         NOT NULL,
    author          JSONB        NOT NULL,
    author_id       UUID         NOT NULL,
    title           VARCHAR(300) NOT NULL,
    content         TEXT,
    post_type       VARCHAR(20)  NOT NULL DEFAULT 'TEXT',
    link_urls       JSONB        NOT NULL DEFAULT '[]'::jsonb,
    media           JSONB        NOT NULL DEFAULT '[]'::jsonb,
    score           BIGINT       NOT NULL DEFAULT 0,
    comment_count   BIGINT       NOT NULL DEFAULT 0,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_posts_community ON posts (community_id);
CREATE INDEX idx_posts_author ON posts (author_id);
CREATE INDEX idx_posts_created ON posts (created_at DESC);
CREATE INDEX idx_posts_score ON posts (score DESC);
