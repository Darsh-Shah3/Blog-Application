ALTER TABLE media_files
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(50),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50);

UPDATE media_files
SET created_by = COALESCE(created_by, 'user-' || uploader_id::text),
    updated_by = COALESCE(updated_by, 'user-' || uploader_id::text)
WHERE created_by IS NULL OR updated_by IS NULL;
