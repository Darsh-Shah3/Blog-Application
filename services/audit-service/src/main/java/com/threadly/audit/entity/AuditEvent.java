package com.threadly.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "occurred_at", nullable = false)
    @Builder.Default
    private Instant occurredAt = Instant.now();

    @Column(name = "service_name", nullable = false, length = 80)
    private String serviceName;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "actor_username", length = 50)
    private String actorUsername;

    @Column(length = 500)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    /** JSON snapshot before mutation (UPDATE). */
    @Column(name = "before_data", columnDefinition = "TEXT")
    private String beforeData;

    /** JSON snapshot after mutation (CREATE/UPDATE). */
    @Column(name = "after_data", columnDefinition = "TEXT")
    private String afterData;

    @Column(name = "request_id", length = 64)
    private String requestId;
}
