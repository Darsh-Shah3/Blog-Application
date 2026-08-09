package com.threadly.community.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dm_threads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmThread {

    @Id
    private UUID id;

    @Column(name = "user_low", nullable = false)
    private UUID userLow;

    @Column(name = "user_high", nullable = false)
    private UUID userHigh;

    @Column(name = "user_low_username", nullable = false, length = 100)
    private String userLowUsername;

    @Column(name = "user_high_username", nullable = false, length = 100)
    private String userHighUsername;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "last_message_preview", length = 200)
    private String lastMessagePreview;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    void ensureId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public boolean involves(UUID userId) {
        return userId != null && (userId.equals(userLow) || userId.equals(userHigh));
    }

    public UUID peerOf(UUID me) {
        if (me == null) {
            return null;
        }
        if (me.equals(userLow)) {
            return userHigh;
        }
        if (me.equals(userHigh)) {
            return userLow;
        }
        return null;
    }

    public String peerUsernameOf(UUID me) {
        if (me == null) {
            return "user";
        }
        if (me.equals(userLow)) {
            return userHighUsername;
        }
        if (me.equals(userHigh)) {
            return userLowUsername;
        }
        return "user";
    }
}
