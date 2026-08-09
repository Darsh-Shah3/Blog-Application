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

    public static final String COUNT_BY_RESOURCE_TYPE_SINCE =
            "select e.resourceType, count(e) from AuditEvent e "
                    + "where e.occurredAt >= :from "
                    + "group by e.resourceType "
                    + "order by count(e) desc";

    public static final String COUNT_TOP_ACTORS_SINCE =
            "select e.actorUsername, count(e) from AuditEvent e "
                    + "where e.occurredAt >= :from "
                    + "and e.actorUsername is not null and e.actorUsername <> '' "
                    + "group by e.actorUsername "
                    + "order by count(e) desc";

    /** Native — daily counts for trend charts (UTC calendar days). */
    public static final String COUNT_PER_DAY_SINCE_NATIVE =
            "select to_char(date_trunc('day', occurred_at at time zone 'UTC'), 'YYYY-MM-DD') as d, "
                    + "count(*) "
                    + "from audit_events "
                    + "where occurred_at >= :from "
                    + "group by 1 order by 1";

    /** Native — hour-of-day activity (UTC) for last window. */
    public static final String COUNT_PER_HOUR_OF_DAY_SINCE_NATIVE =
            "select cast(extract(hour from occurred_at at time zone 'UTC') as int) as h, "
                    + "count(*) "
                    + "from audit_events "
                    + "where occurred_at >= :from "
                    + "group by 1 order by 1";
}
