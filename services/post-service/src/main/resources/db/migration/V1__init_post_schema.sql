CREATE TABLE posts (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT       NOT NULL,
    author_id       BIGINT       NOT NULL,
    title           VARCHAR(300) NOT NULL,
    content         TEXT,
    post_type       VARCHAR(20)  NOT NULL DEFAULT 'TEXT',
    link_url        VARCHAR(1000),
    media_id        BIGINT,
    score           BIGINT       NOT NULL DEFAULT 0,
    comment_count   BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_posts_community ON posts(community_id, created_at DESC);
CREATE INDEX idx_posts_score ON posts(score DESC);
CREATE INDEX idx_posts_author ON posts(author_id);
CREATE INDEX idx_posts_title_search ON posts USING gin (to_tsvector('english', title));
