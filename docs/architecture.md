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

## Ops add-ons (layered without rewriting domains)

- **Redis** — gateway rate limits, JWT blacklist, hot-read caches, unread counters ([redis.md](./redis.md))
- **Kubernetes** — local manifests + Ingress LB ([kubernetes.md](./kubernetes.md))
- **Object storage** — `MEDIA_STORAGE=s3` / MinIO ([media-storage.md](./media-storage.md))
- **CI** — GitHub Actions tests + GHCR push (CD deploy still deferred)
- **Kafka** — still optional for vote/karma fan-out later
