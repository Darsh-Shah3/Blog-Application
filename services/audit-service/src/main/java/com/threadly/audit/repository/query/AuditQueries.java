package com.threadly.audit.repository.query;

/**
 * JPQL catalog for audit-service custom repository queries.
 */
public final class AuditQueries {

    private AuditQueries() {
    }

    public static final String EVENT_SEARCH =
            "select e from AuditEvent e "
                    + "where (:service is null or e.serviceName = :service) "
                    + "and (:action is null or e.action = :action) "
                    + "and (:resourceType is null or e.resourceType = :resourceType) "
                    + "and (:actorUsername is null or lower(e.actorUsername) = lower(:actorUsername)) "
                    + "and (:actorUserId is null or e.actorUserId = :actorUserId) "
                    + "and (:q is null or lower(e.summary) like lower(concat('%', :q, '%')) "
                    + "     or lower(e.resourceId) like lower(concat('%', :q, '%'))) "
                    + "and (:from is null or e.occurredAt >= :from) "
                    + "and (:to is null or e.occurredAt <= :to)";

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
