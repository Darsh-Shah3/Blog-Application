CREATE EXTENSION IF NOT EXISTS pgcrypto;

DROP TABLE IF EXISTS media_files CASCADE;

CREATE TABLE media_files (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_name    VARCHAR(500) NOT NULL,
    stored_name      VARCHAR(255) NOT NULL UNIQUE,
    content_type     VARCHAR(200) NOT NULL,
    size_bytes       BIGINT       NOT NULL,
    uploader         JSONB        NOT NULL,
    uploader_id      UUID         NOT NULL,
    media_kind       VARCHAR(20)  NOT NULL DEFAULT 'OTHER',
    created_by       VARCHAR(50),
    updated_by       VARCHAR(50),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_media_uploader ON media_files (uploader_id);
