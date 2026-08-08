package com.threadly.post.repository.query;

/**
 * Native SQL for ranking feeds (true hot / trending / for-you) and FTS search.
 * Hot rank: (score + 1) / (hours_since_post + 2)^1.5
 * Visible posts: PUBLISHED, or due SCHEDULED (publisher job also promotes them).
 */
public final class PostQueries {

    private PostQueries() {
    }

    public static final String VISIBLE =
            "(p.status = 'PUBLISHED' OR (p.status = 'SCHEDULED' AND p.scheduled_at IS NOT NULL AND p.scheduled_at <= NOW()))";

    public static final String HOT_EXPR =
            "((GREATEST(p.score, 0) + 1.0) / POWER("
                    + "GREATEST(EXTRACT(EPOCH FROM (NOW() - p.created_at)) / 3600.0, 0) + 2.0, 1.5))";

    public static final String FOR_YOU_HOT_EXPR =
            "(((GREATEST(p.score, 0) + 1.0) / POWER("
                    + "GREATEST(EXTRACT(EPOCH FROM (NOW() - p.created_at)) / 3600.0, 0) + 2.0, 1.5))"
                    + " * CASE WHEN p.author_id = ANY(CAST(:followingIds AS uuid[])) THEN 1.25 ELSE 1.0 END)";

    public static final String SEARCH_BY_TITLE_JPQL =
            "select p from Post p where lower(p.title) like lower(concat('%', :q, '%')) "
                    + "and p.status = 'PUBLISHED'";

    public static final String FIND_ALL_HOT =
            "SELECT p.* FROM posts p WHERE " + VISIBLE + " ORDER BY " + HOT_EXPR + " DESC, p.created_at DESC";

    public static final String FIND_ALL_HOT_COUNT = "SELECT count(*) FROM posts p WHERE " + VISIBLE;

    public static final String FIND_BY_COMMUNITY_HOT =
            "SELECT p.* FROM posts p WHERE " + VISIBLE + " AND p.community_id = :communityId ORDER BY "
                    + HOT_EXPR + " DESC, p.created_at DESC";

    public static final String FIND_BY_COMMUNITY_HOT_COUNT =
            "SELECT count(*) FROM posts p WHERE " + VISIBLE + " AND p.community_id = :communityId";

    public static final String FIND_BY_AUTHOR_HOT =
            "SELECT p.* FROM posts p WHERE " + VISIBLE + " AND p.author_id = :authorId ORDER BY "
                    + HOT_EXPR + " DESC, p.created_at DESC";

    public static final String FIND_BY_AUTHOR_HOT_COUNT =
            "SELECT count(*) FROM posts p WHERE " + VISIBLE + " AND p.author_id = :authorId";

    public static final String FIND_BY_COMMUNITIES_HOT =
            "SELECT p.* FROM posts p WHERE " + VISIBLE
                    + " AND p.community_id = ANY(CAST(:communityIds AS uuid[])) ORDER BY "
                    + HOT_EXPR + " DESC, p.created_at DESC";

    public static final String FIND_BY_COMMUNITIES_HOT_COUNT =
            "SELECT count(*) FROM posts p WHERE " + VISIBLE
                    + " AND p.community_id = ANY(CAST(:communityIds AS uuid[]))";

    public static final String TRENDING =
            "SELECT p.* FROM posts p WHERE " + VISIBLE + " AND p.created_at >= :since "
                    + "ORDER BY p.score DESC, p.comment_count DESC, p.created_at DESC";

    public static final String TRENDING_COUNT =
            "SELECT count(*) FROM posts p WHERE " + VISIBLE + " AND p.created_at >= :since";

    public static final String TRENDING_IN_COMMUNITIES =
            "SELECT p.* FROM posts p WHERE " + VISIBLE + " AND p.created_at >= :since "
                    + "AND p.community_id = ANY(CAST(:communityIds AS uuid[])) "
                    + "ORDER BY p.score DESC, p.comment_count DESC, p.created_at DESC";

    public static final String TRENDING_IN_COMMUNITIES_COUNT =
            "SELECT count(*) FROM posts p WHERE " + VISIBLE + " AND p.created_at >= :since "
                    + "AND p.community_id = ANY(CAST(:communityIds AS uuid[]))";

    public static final String FOR_YOU =
            "SELECT p.* FROM posts p WHERE " + VISIBLE + " AND ("
                    + "( cardinality(CAST(:communityIds AS uuid[])) > 0 AND p.community_id = ANY(CAST(:communityIds AS uuid[])) ) "
                    + "OR ( cardinality(CAST(:followingIds AS uuid[])) > 0 AND p.author_id = ANY(CAST(:followingIds AS uuid[])) ) "
                    + ") ORDER BY " + FOR_YOU_HOT_EXPR + " DESC, p.created_at DESC";

    public static final String FOR_YOU_COUNT =
            "SELECT count(*) FROM posts p WHERE " + VISIBLE + " AND ("
                    + "( cardinality(CAST(:communityIds AS uuid[])) > 0 AND p.community_id = ANY(CAST(:communityIds AS uuid[])) ) "
                    + "OR ( cardinality(CAST(:followingIds AS uuid[])) > 0 AND p.author_id = ANY(CAST(:followingIds AS uuid[])) ) )";

    public static final String SEARCH_FTS =
            "SELECT p.* FROM posts p WHERE " + VISIBLE + " AND to_tsvector('english', coalesce(p.title, '')) "
                    + "@@ plainto_tsquery('english', :q) "
                    + "ORDER BY ts_rank(to_tsvector('english', coalesce(p.title, '')), plainto_tsquery('english', :q)) DESC, p.created_at DESC";

    public static final String SEARCH_FTS_COUNT =
            "SELECT count(*) FROM posts p WHERE " + VISIBLE + " AND to_tsvector('english', coalesce(p.title, '')) "
                    + "@@ plainto_tsquery('english', :q)";

    public static final String RELATED =
            "SELECT p.* FROM posts p "
                    + "WHERE " + VISIBLE + " AND p.community_id = :communityId AND p.id <> :excludeId "
                    + "ORDER BY ("
                    + "  CASE WHEN to_tsvector('english', coalesce(p.title, '')) "
                    + "    @@ plainto_tsquery('english', :titleTerms) THEN 2.0 ELSE 1.0 END "
                    + "  * " + HOT_EXPR
                    + ") DESC, p.created_at DESC "
                    + "LIMIT :limit";
}
