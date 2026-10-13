-- WhatsApp-style edit + delete-for-me / delete-for-everyone on chat messages

ALTER TABLE dm_messages
    ADD COLUMN IF NOT EXISTS edited_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deleted_for_everyone BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS hidden_for UUID[] NOT NULL DEFAULT '{}';

ALTER TABLE community_messages
    ADD COLUMN IF NOT EXISTS edited_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deleted_for_everyone BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS hidden_for UUID[] NOT NULL DEFAULT '{}';
