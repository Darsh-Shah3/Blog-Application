package com.threadly.post.repository.query;

/**
 * JPQL catalog for post-service custom repository queries.
 */
public final class PostQueries {

    private PostQueries() {
    }

    public static final String SEARCH_BY_TITLE =
            "select p from Post p where lower(p.title) like lower(concat('%', :q, '%'))";
}
