package com.threadly.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String displayName;
    private String bio;
    private Long karma;
    private Set<String> roles;
    /** Derived rights from roles (also stored in JWT {@code permissions} claim). */
    private Set<String> permissions;
    private Instant createdAt;
    private String createdBy;
    private String updatedBy;
}
