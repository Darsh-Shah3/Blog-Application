package com.threadly.audit.controller;

import com.threadly.audit.dto.AuditEventResponse;
import com.threadly.audit.dto.CreateAuditEventRequest;
import com.threadly.audit.dto.PlatformReportResponse;
import com.threadly.audit.service.AuditService;
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
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 30) Pageable pageable) {
        return ResponseEntity.ok(auditService.search(
                service, action, resourceType, actorUsername, actorUserId, q, from, to, pageable));
    }

    /** Admin platform overview report. */
    @GetMapping("/api/v1/reports/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlatformReportResponse> overview() {
        return ResponseEntity.ok(auditService.platformReport());
    }

    /** Activity report (same 7-day breakdown nested in overview; exposed separately for API clarity). */
    @GetMapping("/api/v1/reports/activity")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlatformReportResponse> activity() {
        return ResponseEntity.ok(auditService.platformReport());
    }
}
