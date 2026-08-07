package com.threadly.audit.repository;

import com.threadly.audit.entity.AuditEvent;
import com.threadly.audit.repository.query.AuditQueries;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    @Query(AuditQueries.EVENT_SEARCH)
    Page<AuditEvent> search(
            @Param("service") String service,
            @Param("action") String action,
            @Param("resourceType") String resourceType,
            @Param("actorUsername") String actorUsername,
            @Param("actorUserId") Long actorUserId,
            @Param("q") String q,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query(AuditQueries.COUNT_BY_ACTION_SINCE)
    List<Object[]> countByActionSince(@Param("from") Instant from);

    @Query(AuditQueries.COUNT_BY_SERVICE_SINCE)
    List<Object[]> countByServiceSince(@Param("from") Instant from);

    @Query(AuditQueries.COUNT_BY_RESOURCE_AND_ACTION_SINCE)
    List<Object[]> countByResourceAndActionSince(@Param("from") Instant from);

    long countByOccurredAtAfter(Instant after);
}
