package com.threadly.auth.rbac;

/**
 * Fine-grained rights carried in JWT {@code permissions} claim and as granted authorities.
 * Domain services may also derive privilege from roles; auth-service is source of truth.
 */
public enum Permission {
    CONTENT_CREATE,
    CONTENT_DELETE_OWN,
    CONTENT_DELETE_ANY,
    COMMUNITY_CREATE,
    COMMUNITY_DELETE,
    VOTE_CAST,
    USER_MANAGE,
    ROLE_ASSIGN
}
