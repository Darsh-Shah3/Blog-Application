-- Scheduled posts support
ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS scheduled_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED';

CREATE INDEX IF NOT EXISTS idx_posts_status_scheduled
    ON posts (status, scheduled_at)
    WHERE status = 'SCHEDULED';

CREATE INDEX IF NOT EXISTS idx_posts_status_created
    ON posts (status, created_at DESC);
