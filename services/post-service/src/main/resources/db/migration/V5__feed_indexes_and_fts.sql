-- Composite indexes for feed filters + hot/trending windows; full-text title search.
CREATE INDEX IF NOT EXISTS idx_posts_community_created ON posts (community_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_posts_community_score ON posts (community_id, score DESC);
CREATE INDEX IF NOT EXISTS idx_posts_author_created ON posts (author_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_posts_created_score ON posts (created_at DESC, score DESC);

CREATE INDEX IF NOT EXISTS idx_posts_title_fts ON posts USING GIN (to_tsvector('english', coalesce(title, '')));
