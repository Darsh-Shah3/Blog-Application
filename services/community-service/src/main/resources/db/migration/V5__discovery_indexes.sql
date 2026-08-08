-- Recent memberships for community discovery ranking.
CREATE INDEX IF NOT EXISTS idx_memberships_joined_at ON community_memberships (joined_at DESC);
CREATE INDEX IF NOT EXISTS idx_memberships_community_joined ON community_memberships (community_id, joined_at DESC);
CREATE INDEX IF NOT EXISTS idx_communities_member_count ON communities (member_count DESC);
