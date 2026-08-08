# Prometheus & Grafana for Threadly

This stack records **how healthy and fast** your microservices are. End users never open Grafana; **admins** can also see the same metrics inside Threadly at **Admin → Analytics**.

---

## How the pieces connect

```
┌──────────────────┐   scrape GET /actuator/prometheus every 15s
│ Spring services  │ ──────────────────────────────────────────┐
│ (Micrometer)     │                                           │
└──────────────────┘                                           ▼
                                                    ┌────────────────────┐
                                                    │   Prometheus       │
                                                    │   :9090            │
                                                    │ stores time-series │
                                                    └─────────┬──────────┘
                           Grafana dashboards                 │
                           (query PromQL)                     │
                                    ▲                         │
                                    │                         │
                         ┌──────────┴──────────┐   audit-service GET
                         │ Grafana :3001       │   /api/v1/query*
                         │ (ops deep-dive)     │◄──────────────────────┐
                         └─────────────────────┘                       │
                                                                       │
                         ┌─────────────────────┐                       │
                         │ Threadly Admin UI   │  GET /api/v1/reports/ │
                         │ /admin/analytics    │  ops-metrics          │
                         └──────────▲──────────┘                       │
                                    │                                  │
                         ┌──────────┴──────────┐                       │
                         │ audit-service       │───────────────────────┘
                         │ PrometheusClient    │
                         └─────────────────────┘
```

1. **Every Java service** exposes metrics at `/actuator/prometheus` (request counts, latency histograms, JVM memory).
2. **Prometheus** pulls those endpoints on a schedule (`infra/prometheus/prometheus.yml`) and stores samples.
3. **Grafana** is only a UI: its data source points at `http://prometheus:9090`. It does **not** talk to services directly.
4. **Admin Analytics** (on the website) asks **audit-service**, which runs the **same PromQL queries** against Prometheus and returns JSON for in-app charts. No redirect to Grafana.

---

## URLs (local Docker)

| Tool | URL | Login |
|------|-----|--------|
| Grafana | http://localhost:3001 | `admin` / `admin` (from `.env`) |
| Prometheus | http://localhost:9090 | — |
| Admin Analytics | http://localhost:3000/admin/analytics | admin JWT |
| Metrics sample | http://localhost:8083/actuator/prometheus | — |

---

## What to look at (recommended)

| Panel / metric | Meaning |
|----------------|---------|
| **Request rate** by service | Traffic distribution; spikes after releases |
| **p95 latency** by service | User-felt lag (95% of requests faster than this) |
| **5xx rate** | Server failures |
| **JVM heap** | Memory pressure / leaks |
| **Feed / comments p95** | Ranking & thread APIs specifically |
| **`up` targets** | Whether Prometheus can scrape each service |

---

## Quick Grafana walkthrough

1. `docker compose up -d` (apps + prometheus + grafana healthy).
2. Open Grafana → login → **Dashboards → Threadly → Threadly Microservices Overview**.
3. Generate load: browse feed, create posts, comment.
4. Watch request rate and latency rise; errors should stay near zero.

Optional: **Explore** → select Prometheus → try:

```promql
sum(rate(http_server_requests_seconds_count{application="post-service"}[1m])) by (uri)
```

---

## Admin Analytics page

- Path: `/admin/analytics` (admin role only).
- Data: `GET /api/v1/reports/ops-metrics?hours=6`.
- If charts are empty, check Prometheus scrape targets at http://localhost:9090/targets and that `PROMETHEUS_URL=http://prometheus:9090` is set on `audit-service`.

---

## Config files

| Path | Role |
|------|------|
| `infra/prometheus/prometheus.yml` | Scrape jobs for all services |
| `infra/grafana/provisioning/datasources/datasource.yml` | Grafana → Prometheus URL |
| `infra/grafana/provisioning/dashboards/` | Provisioned dashboards |
| `services/*/application.yml` → `management.endpoints` | Expose prometheus endpoint |
