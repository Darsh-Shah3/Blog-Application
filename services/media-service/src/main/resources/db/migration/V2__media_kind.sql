-- File family for UI + enforcement (IMAGE/VIDEO/AUDIO/DOCUMENT/ARCHIVE/OTHER)
ALTER TABLE media_files
    ADD COLUMN IF NOT EXISTS media_kind VARCHAR(20) NOT NULL DEFAULT 'OTHER';

CREATE INDEX IF NOT EXISTS idx_media_uploader ON media_files (uploader_id);
CREATE INDEX IF NOT EXISTS idx_media_kind ON media_files (media_kind);
