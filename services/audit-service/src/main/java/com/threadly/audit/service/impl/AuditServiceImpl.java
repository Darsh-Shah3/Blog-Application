package com.threadly.audit.service.impl;

import com.threadly.audit.client.StatsClient;
import com.threadly.audit.dto.AuditEventResponse;
import com.threadly.audit.dto.CreateAuditEventRequest;
import com.threadly.audit.dto.PlatformReportResponse;
import com.threadly.audit.entity.AuditEvent;
import com.threadly.audit.repository.AuditEventRepository;
import com.threadly.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditEventRepository repository;
    private final StatsClient statsClient;

    @Override
    @Transactional
    public AuditEventResponse record(CreateAuditEventRequest request) {
        AuditEvent event = AuditEvent.builder()
                .serviceName(request.getServiceName().trim())
                .action(request.getAction().trim().toUpperCase())
                .resourceType(request.getResourceType().trim().toUpperCase())
                .resourceId(request.getResourceId())
                .actorUserId(request.getActorUserId())
                .actorUsername(request.getActorUsername())
                .summary(request.getSummary())
                .metadata(request.getMetadata())
                .beforeData(request.getBeforeData())
                .afterData(request.getAfterData())
                .requestId(request.getRequestId())
                .occurredAt(Instant.now())
                .build();
        event = repository.save(event);
        log.debug("Audit recorded id={} action={} resource={}/{}",
                event.getId(), event.getAction(), event.getResourceType(), event.getResourceId());
        return toResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEventResponse> search(
            String service, String action, String resourceType,
            String actorUsername, java.util.UUID actorUserId, String q,
            Instant from, Instant to, Pageable pageable) {
        // Empty-string / open bounds — never null Instant/UUID — so Postgres always has a typed param.
        String svc = blankToEmpty(service);
        String act = blankToEmpty(action);
        if (!act.isEmpty()) {
            act = act.toUpperCase();
        }
        String rt = blankToEmpty(resourceType);
        if (!rt.isEmpty()) {
            rt = rt.toUpperCase();
        }
        String actorName = blankToEmpty(actorUsername);
        if (!actorName.isEmpty()) {
            actorName = actorName.toLowerCase(java.util.Locale.ROOT);
        }
        String pattern = "";
        if (q != null && !q.isBlank()) {
            pattern = "%" + q.trim().toLowerCase(java.util.Locale.ROOT) + "%";
        }
        Instant fromBound = from != null ? from : Instant.EPOCH;
        Instant toBound = to != null ? to : Instant.parse("9999-12-31T23:59:59Z");
        UUID actorId = actorUserId != null ? actorUserId : new UUID(0L, 0L);
        return repository.search(svc, act, rt, actorName, actorUserId != null, actorId,
                pattern, fromBound, toBound, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformReportResponse platformReport() {
        Instant now = Instant.now();
        Instant d1 = now.minus(1, ChronoUnit.DAYS);
        Instant d7 = now.minus(7, ChronoUnit.DAYS);

        Map<String, Long> actions = toMap(repository.countByActionSince(d7));
        Map<String, Long> services = toMap(repository.countByServiceSince(d7));
        List<PlatformReportResponse.ResourceActionCount> ra = repository.countByResourceAndActionSince(d7)
                .stream()
                .map(row -> PlatformReportResponse.ResourceActionCount.builder()
                        .resourceType(String.valueOf(row[0]))
                        .action(String.valueOf(row[1]))
                        .count(((Number) row[2]).longValue())
                        .build())
                .toList();

        StatsClient.PlatformCounts counts = statsClient.fetchPlatformCounts();

        return PlatformReportResponse.builder()
                .totalAuditEvents(repository.count())
                .auditEventsLast24h(repository.countByOccurredAtAfter(d1))
                .auditEventsLast7d(repository.countByOccurredAtAfter(d7))
                .actionsLast7d(actions)
                .servicesLast7d(services)
                .resourceActionsLast7d(ra)
                .activeUsers(counts.users())
                .totalPosts(counts.posts())
                .totalCommunities(counts.communities())
                .totalComments(counts.comments())
                .generatedAt(now.toString())
                .build();
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return map;
    }

    private String blankToEmpty(String s) {
        return s == null || s.isBlank() ? "" : s.trim();
    }

    private AuditEventResponse toResponse(AuditEvent e) {
        return AuditEventResponse.builder()
                .id(e.getId())
                .occurredAt(e.getOccurredAt())
                .serviceName(e.getServiceName())
                .action(e.getAction())
                .resourceType(e.getResourceType())
                .resourceId(e.getResourceId())
                .actorUserId(e.getActorUserId())
                .actorUsername(e.getActorUsername())
                .summary(e.getSummary())
                .metadata(e.getMetadata())
                .beforeData(e.getBeforeData())
                .afterData(e.getAfterData())
                .requestId(e.getRequestId())
                .build();
    }
}
