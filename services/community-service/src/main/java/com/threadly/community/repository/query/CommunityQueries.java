package com.threadly.community.repository.query;

public final class CommunityQueries {

    private CommunityQueries() {
    }

    /** Rank by recent joins (7d), then total members, then freshness. */
    public static final String DISCOVER =
            "SELECT c.* FROM communities c ORDER BY ("
                    + "SELECT COUNT(*) FROM community_memberships m "
                    + "WHERE m.community_id = c.id AND m.joined_at >= NOW() - INTERVAL '7 days'"
                    + ") DESC, c.member_count DESC, c.created_at DESC";

    public static final String DISCOVER_COUNT = "SELECT count(*) FROM communities";
}
