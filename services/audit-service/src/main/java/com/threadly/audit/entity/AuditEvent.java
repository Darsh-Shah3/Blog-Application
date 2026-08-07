package com.threadly.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    @Builder.Default
    private Instant occurredAt = Instant.now();

    @Column(name = "service_name", nullable = false, length = 80)
    private String serviceName;

    /** CREATE | UPDATE | DELETE | LOGIN | OTHER */
    @Column(nullable = false, length = 20)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_username", length = 50)
    private String actorUsername;

    @Column(length = 500)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "request_id", length = 64)
    private String requestId;
}
