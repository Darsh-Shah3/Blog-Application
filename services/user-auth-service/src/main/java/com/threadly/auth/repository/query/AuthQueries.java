package com.threadly.auth.repository.query;

/**
 * JPQL catalog for user-auth-service.
 */
public final class AuthQueries {

    private AuthQueries() {
    }

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
            "select count(u) from User u join u.userRoles ur "
                    + "where ur.roleName = :roleName and u.deletedAt is null";

    public static final String PASSWORD_RESET_DELETE_UNUSED_FOR_USER =
            "delete from PasswordResetToken t where t.userId = :userId and t.used = false";
}
