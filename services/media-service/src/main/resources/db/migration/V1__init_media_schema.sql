CREATE TABLE media_files (
    id              BIGSERIAL PRIMARY KEY,
    original_name   VARCHAR(500) NOT NULL,
    stored_name     VARCHAR(200) NOT NULL UNIQUE,
    content_type    VARCHAR(100) NOT NULL,
    size_bytes      BIGINT       NOT NULL,
    uploader_id     BIGINT       NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
