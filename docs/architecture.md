# Architecture

## Style

Threadly uses a **modular monolith-of-services** laid out as independent Spring Boot apps with:

- Database-per-service on one Postgres server
- Edge API gateway for routing, CORS, and JWT
- Synchronous service-to-service HTTP for enrichment and vote side effects
- Central metrics scrape (Prometheus) + dashboards (Grafana)

## Request path

1. Browser calls only `NEXT_PUBLIC_API_URL` (gateway).
2. Gateway validates JWT (when present), strips forged identity headers, injects `X-User-Id` / `X-User-Roles` / `X-Request-Id`.
3. Public GET routes work anonymously (feeds, community pages, comment trees).
4. Mutations require a valid bearer token.

## Vote / karma flow

```
Client → gateway → vote-service
  → verifies POST/COMMENT exists (post-service / comment-service)
  → writes vote row (vote_db)
  → POST score delta to post/comment internal endpoints
  → POST karma delta to user-auth internal endpoint
```

Score/karma updates are **best-effort** across services for the MVP (documented eventual consistency).

## Observability

Each service exposes:

- `/actuator/health`
- `/actuator/prometheus`

Prometheus scrapes all jobs defined in `infra/prometheus/prometheus.yml`. Grafana provisions Prometheus and a starter dashboard.

## Why not Kafka/K8s yet

Compose + gateway + Flyway + Prometheus already demonstrates production-adjacent microservices practices. Kafka/K8s can be layered later without rewriting domain services.
