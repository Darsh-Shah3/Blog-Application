package com.threadly.community.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "community_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityMessage {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> community;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> author;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "deleted_for_everyone", nullable = false)
    @Builder.Default
    private boolean deletedForEveryone = false;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "hidden_for", columnDefinition = "uuid[]", nullable = false)
    @Builder.Default
    private List<UUID> hiddenFor = new ArrayList<>();

    public boolean isHiddenFor(UUID userId) {
        return userId != null && hiddenFor != null && hiddenFor.contains(userId);
    }

    public void hideFor(UUID userId) {
        if (userId == null) {
            return;
        }
        if (hiddenFor == null) {
            hiddenFor = new ArrayList<>();
        } else if (!(hiddenFor instanceof ArrayList)) {
            hiddenFor = new ArrayList<>(hiddenFor);
        }
        if (!hiddenFor.contains(userId)) {
            hiddenFor.add(userId);
        }
    }

    @PrePersist
    void ensureId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (hiddenFor == null) {
            hiddenFor = new ArrayList<>();
        }
    }
}
