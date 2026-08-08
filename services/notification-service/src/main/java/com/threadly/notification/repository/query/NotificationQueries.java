package com.threadly.notification.repository.query;

/**
 * JPQL catalog for notification-service custom repository queries.
 * <p>
 * Search pattern for {@code :q} must be pre-built in Java as {@code %term%} (already lower-cased).
 * Avoid SQL {@code concat('%', :q, '%')} — Hibernate 6 on Postgres can bind that as bytea
 * and then {@code lower(...)} fails with "function lower(bytea) does not exist".
 */
public final class NotificationQueries {

    private NotificationQueries() {
    }

    public static final String SEARCH_FOR_USER =
            "select n from Notification n "
                    + "where n.userId = :userId "
                    + "and (:unreadOnly = false or n.readAt is null) "
                    + "and (:type is null or n.type = :type) "
                    + "and (:q is null or lower(n.title) like :q "
                    + "     or (n.body is not null and lower(n.body) like :q) "
                    + "     or (n.summary is not null and lower(n.summary) like :q) "
                    + "     or (n.postTitle is not null and lower(n.postTitle) like :q))";
}
