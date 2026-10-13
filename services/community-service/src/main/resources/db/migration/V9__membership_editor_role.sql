-- Community creators can grant EDITOR role so others may update name/description.
ALTER TABLE community_memberships
    ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'MEMBER';

CREATE INDEX IF NOT EXISTS idx_memberships_community_role
    ON community_memberships (community_id, role);
