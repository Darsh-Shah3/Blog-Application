ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(50),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50);

UPDATE posts p
SET created_by = COALESCE(created_by, 'user-' || author_id::text),
    updated_by = COALESCE(updated_by, 'user-' || author_id::text)
WHERE created_by IS NULL OR updated_by IS NULL;
