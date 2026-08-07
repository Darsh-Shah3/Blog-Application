package com.threadly.notification.repository.query;

/**
 * JPQL catalog for notification-service custom repository queries.
 */
public final class NotificationQueries {

    private NotificationQueries() {
    }

    public static final String SEARCH_FOR_USER =
            "select n from Notification n "
                    + "where n.userId = :userId "
                    + "and (:unreadOnly = false or n.readAt is null) "
                    + "and (:type is null or n.type = :type) "
                    + "and (:q is null or lower(n.title) like lower(concat('%', :q, '%')) "
                    + "     or lower(n.body) like lower(concat('%', :q, '%')))";
}
