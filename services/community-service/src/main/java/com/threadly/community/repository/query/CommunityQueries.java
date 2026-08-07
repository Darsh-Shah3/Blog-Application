package com.threadly.community.repository.query;

/**
 * JPQL catalog for community-service.
 * <p>
 * Currently repositories use Spring Data method names only
 * (e.g. {@code findBySlug}, {@code existsBySlug}). Add JPQL constants here
 * whenever a custom {@code @Query} is needed.
 */
public final class CommunityQueries {

    private CommunityQueries() {
    }

    // Reserved for future custom JPQL, for example:
    // public static final String SEARCH_BY_NAME =
    //         "select c from Community c where lower(c.name) like lower(concat('%', :q, '%'))";
}
