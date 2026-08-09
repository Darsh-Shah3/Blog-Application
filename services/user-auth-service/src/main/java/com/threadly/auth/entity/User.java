package com.threadly.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Uniqueness is DB partial index (active rows only) — not JPA unique. */
    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(length = 500)
    private String bio;

    /** Media-service file id for profile display picture; content via /api/v1/media/{id}/content. */
    @Column(name = "avatar_media_id")
    private UUID avatarMediaId;

    /**
     * Reputation score (Reddit-style “karma”). Increased/decreased by the vote-service
     * via internal API when others upvote/downvote this user's posts or comments.
     */
    @Column(nullable = false)
    @Builder.Default
    private Long karma = 0L;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    /** Soft-delete marker; null = active. */
    @Column(name = "deleted_at")
    private java.time.Instant deletedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private java.time.Instant createdAt = java.time.Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private java.time.Instant updatedAt = java.time.Instant.now();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private Set<UserRole> userRoles = new HashSet<>();

    /** Role entities derived from denormalized user_roles rows. */
    public Set<Role> getRoles() {
        if (userRoles == null || userRoles.isEmpty()) {
            return new HashSet<>();
        }
        return userRoles.stream().map(UserRole::getRole).collect(Collectors.toCollection(HashSet::new));
    }

    public void replaceRoles(Set<Role> roles) {
        if (userRoles == null) {
            userRoles = new HashSet<>();
        }
        userRoles.clear();
        if (roles == null) {
            return;
        }
        for (Role role : roles) {
            userRoles.add(UserRole.builder()
                    .user(this)
                    .role(role)
                    .username(this.username)
                    .roleName(role.getName())
                    .build());
        }
    }

    public void addRole(Role role) {
        if (userRoles == null) {
            userRoles = new HashSet<>();
        }
        boolean exists = userRoles.stream().anyMatch(ur -> ur.getRole().getId().equals(role.getId()));
        if (!exists) {
            userRoles.add(UserRole.builder()
                    .user(this)
                    .role(role)
                    .username(this.username)
                    .roleName(role.getName())
                    .build());
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = java.time.Instant.now();
    }
}
