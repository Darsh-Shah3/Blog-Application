package com.threadly.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Recipient: {id, username}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> recipient;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 1000)
    private String body;

    /** Creator-provided short summary for cards (YouTube-style secondary line). */
    @Column(length = 500)
    private String summary;

    @Column(name = "link_url", length = 1000)
    private String linkUrl;

    /** Thumbnail / first media URL for the card. */
    @Column(name = "media_url", length = 1000)
    private String mediaUrl;

    @Column(name = "actor_username", length = 50)
    private String actorUsername;

    @Column(name = "post_title", length = 300)
    private String postTitle;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
