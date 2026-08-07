package com.threadly.auth.repository.query;

/**
 * JPQL catalog for user-auth-service.
 * <p>
 * Repositories stay thin method contracts; all custom query text lives here
 * so it is easier to review, reuse, and keep soft-delete rules consistent.
 */
public final class AuthQueries {

    private AuthQueries() {
    }

    // --- User (active rows only: soft-delete friendly) ---

    public static final String USER_FIND_ACTIVE_BY_EMAIL =
            "select u from User u where lower(u.email) = lower(:email) and u.deletedAt is null";

    public static final String USER_FIND_ACTIVE_BY_USERNAME =
            "select u from User u where lower(u.username) = lower(:username) and u.deletedAt is null";

    public static final String USER_EXISTS_ACTIVE_BY_EMAIL =
            "select case when count(u) > 0 then true else false end "
                    + "from User u where lower(u.email) = lower(:email) and u.deletedAt is null";

    public static final String USER_EXISTS_ACTIVE_BY_USERNAME =
            "select case when count(u) > 0 then true else false end "
                    + "from User u where lower(u.username) = lower(:username) and u.deletedAt is null";

    public static final String USER_FIND_ALL_ACTIVE =
            "select u from User u where u.deletedAt is null";

    public static final String USER_SEARCH_ACTIVE =
            "select u from User u "
                    + "where u.deletedAt is null "
                    + "and (lower(u.username) like lower(concat('%', :term, '%')) "
                    + "  or lower(u.email) like lower(concat('%', :term, '%')) "
                    + "  or lower(u.displayName) like lower(concat('%', :term, '%')))";

    public static final String USER_COUNT_ACTIVE =
            "select count(u) from User u where u.deletedAt is null";

    public static final String USER_COUNT_ACTIVE_BY_ROLE_NAME =
            "select count(u) from User u join u.roles r "
                    + "where r.name = :roleName and u.deletedAt is null";

    // --- Follow graph ---

    public static final String FOLLOW_FIND_ACTIVE_FOLLOWERS =
            "select u from User u "
                    + "where u.deletedAt is null "
                    + "and u.id in (select f.followerId from UserFollow f where f.followingId = :userId)";

    // --- Password reset tokens ---

    public static final String PASSWORD_RESET_DELETE_UNUSED_FOR_USER =
            "delete from PasswordResetToken t where t.userId = :userId and t.usedAt is null";
}
