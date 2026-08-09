-- Profile display picture reference (media-service id)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS avatar_media_id UUID NULL;
