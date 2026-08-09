package com.threadly.audit.controller;

import com.threadly.audit.dto.AuditEventResponse;
import com.threadly.audit.dto.CreateAuditEventRequest;
import com.threadly.audit.dto.OpsMetricsResponse;
import com.threadly.audit.dto.PlatformReportResponse;
import com.threadly.audit.service.AuditService;
import com.threadly.audit.service.OpsMetricsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final OpsMetricsService opsMetricsService;

    /** Internal: domain services publish create/update/delete events here. */
    @PostMapping("/api/v1/internal/audit/events")
    @PreAuthorize("hasRole('INTERNAL')")
    public ResponseEntity<AuditEventResponse> recordInternal(@Valid @RequestBody CreateAuditEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auditService.record(request));
    }

    /** Admin: filter/search audit trail. */
    @GetMapping("/api/v1/audit/events")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditEventResponse>> search(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String actorUsername,
            @RequestParam(required = false) java.util.UUID actorUserId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 30) Pageable pageable) {
        return ResponseEntity.ok(auditService.search(
                service, action, resourceType, actorUsername, actorUserId, q, from, to, pageable));
    }

    /** Admin platform overview report (totals + chart series for analytics UI). */
    @GetMapping("/api/v1/reports/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlatformReportResponse> overview(
            @RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(auditService.platformReport(days));
    }

    /** Activity report (same payload as overview; kept for API clarity). */
    @GetMapping("/api/v1/reports/activity")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlatformReportResponse> activity(
            @RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(auditService.platformReport(days));
    }

    /**
     * Live ops metrics for the in-app admin Analytics page.
     * Sourced from Prometheus (same data Grafana uses) so admin graphs stay inside Threadly.
     */
    @GetMapping("/api/v1/reports/ops-metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OpsMetricsResponse> opsMetrics(
            @RequestParam(defaultValue = "6") int hours) {
        return ResponseEntity.ok(opsMetricsService.snapshot(hours));
    }
}
