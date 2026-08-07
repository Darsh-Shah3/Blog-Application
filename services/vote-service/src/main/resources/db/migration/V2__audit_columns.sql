ALTER TABLE votes
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(50),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50);

UPDATE votes
SET created_by = COALESCE(created_by, 'user-' || user_id::text),
    updated_by = COALESCE(updated_by, 'user-' || user_id::text)
WHERE created_by IS NULL OR updated_by IS NULL;
