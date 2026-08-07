package com.threadly.audit.service;

import com.threadly.audit.dto.AuditEventResponse;
import com.threadly.audit.dto.CreateAuditEventRequest;
import com.threadly.audit.dto.PlatformReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface AuditService {
    AuditEventResponse record(CreateAuditEventRequest request);

    Page<AuditEventResponse> search(
            String service, String action, String resourceType,
            String actorUsername, Long actorUserId, String q,
            Instant from, Instant to, Pageable pageable);

    PlatformReportResponse platformReport();
}
