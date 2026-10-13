-- Soft-delete: user "clears" notifications but rows remain for history / audit
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_notif_user_active_created
    ON notifications (user_id, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_notif_user_active_unread
    ON notifications (user_id)
    WHERE read_at IS NULL AND deleted_at IS NULL;
