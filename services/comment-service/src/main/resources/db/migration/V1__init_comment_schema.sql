CREATE TABLE comments (
    id              BIGSERIAL PRIMARY KEY,
    post_id         BIGINT       NOT NULL,
    author_id       BIGINT       NOT NULL,
    parent_id       BIGINT       REFERENCES comments(id) ON DELETE CASCADE,
    content         TEXT         NOT NULL,
    score           BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_post ON comments(post_id, created_at);
CREATE INDEX idx_comments_parent ON comments(parent_id);
