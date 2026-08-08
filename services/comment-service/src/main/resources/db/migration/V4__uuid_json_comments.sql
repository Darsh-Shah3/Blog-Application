-- Comments: UUID PK + post/author/parent JSON snapshots (production-friendly indexes on IDs).
CREATE EXTENSION IF NOT EXISTS pgcrypto;

DROP TABLE IF EXISTS comments CASCADE;

CREATE TABLE comments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post            JSONB        NOT NULL,
    post_id         UUID         NOT NULL,
    author          JSONB        NOT NULL,
    author_id       UUID         NOT NULL,
    parent          JSONB,
    parent_id       UUID,
    content         TEXT         NOT NULL,
    score           BIGINT       NOT NULL DEFAULT 0,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_post ON comments (post_id);
CREATE INDEX idx_comments_post_parent ON comments (post_id, parent_id);
CREATE INDEX idx_comments_post_root_created ON comments (post_id, created_at ASC)
    WHERE parent_id IS NULL;
CREATE INDEX idx_comments_author ON comments (author_id);

ALTER TABLE comments
    ADD CONSTRAINT fk_comments_parent
        FOREIGN KEY (parent_id) REFERENCES comments (id) ON DELETE CASCADE;
