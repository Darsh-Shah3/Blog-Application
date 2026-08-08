package com.threadly.audit.repository.query;

/**
 * JPQL catalog for audit-service custom repository queries.
 * <p>
 * Search pattern for {@code :q} must be pre-built in Java as {@code %term%} (already lower-cased).
 * Avoid SQL {@code concat('%', :q, '%')} — Hibernate 6 on Postgres can bind that as bytea
 * and then {@code lower(...)} fails with "function lower(bytea) does not exist".
 */
public final class AuditQueries {

    private AuditQueries() {
    }

    /**
     * Avoid {@code (:instant is null or ...)} — Postgres cannot infer JDBC types for null Instant/UUID
     * params ("could not determine data type of parameter $N"). Callers pass empty-string / false / open bounds.
     */
    public static final String EVENT_SEARCH =
            "select e from AuditEvent e "
                    + "where (:service = '' or e.serviceName = :service) "
                    + "and (:action = '' or e.action = :action) "
                    + "and (:resourceType = '' or e.resourceType = :resourceType) "
                    + "and (:actorUsername = '' or lower(e.actorUsername) = :actorUsername) "
                    + "and (:hasActorUserId = false or e.actorUserId = :actorUserId) "
                    + "and (:q = '' or ((e.summary is not null and lower(e.summary) like :q) "
                    + "     or (e.resourceId is not null and lower(e.resourceId) like :q))) "
                    + "and e.occurredAt >= :from and e.occurredAt <= :to";

    public static final String COUNT_BY_ACTION_SINCE =
            "select e.action, count(e) from AuditEvent e "
                    + "where e.occurredAt >= :from "
                    + "group by e.action";

    public static final String COUNT_BY_SERVICE_SINCE =
            "select e.serviceName, count(e) from AuditEvent e "
                    + "where e.occurredAt >= :from "
                    + "group by e.serviceName";

    public static final String COUNT_BY_RESOURCE_AND_ACTION_SINCE =
            "select e.resourceType, e.action, count(e) from AuditEvent e "
                    + "where e.occurredAt >= :from "
                    + "group by e.resourceType, e.action";
}
