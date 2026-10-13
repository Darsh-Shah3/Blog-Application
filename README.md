# Threadly

[![CI](https://github.com/Darsh-Shah3/Blog-Application/actions/workflows/ci.yml/badge.svg)](https://github.com/Darsh-Shah3/Blog-Application/actions/workflows/ci.yml)

Reddit-style community platform built as **Java Spring Boot microservices** with a **Next.js + TypeScript** frontend, **PostgreSQL** (database-per-service), **Flyway** migrations, **Docker Compose**, **Redis**, **Kubernetes** manifests, **Prometheus**, and **Grafana**.

> **Threadly** — communities, posts, nested comments, voting, karma, follows, notifications, media uploads, and admin tooling.

## Features

### Platform
- Multi-service backend behind a single **API gateway** (JWT, CORS, rate limits, request IDs)
- **Database-per-service** on one Postgres instance (`auth_db`, `community_db`, `post_db`, …)
- **Communities** with join/leave and membership counts
- **Posts** (text, link, image, file) with scores and comment counts
- **Nested comments** and **up/down voting** with karma side-effects
- **Media uploads** via media-service (local volume; size caps per type)
- **User profiles**, **follow/followers**, and **personalized feed** hooks
- **In-app notifications** (+ optional email when SMTP is configured)
- **Audit log** and **platform reports** for admins
- **RBAC**: `ROLE_USER`, `ROLE_MODERATOR`, `ROLE_ADMIN`
- **Password reset** flow (token + optional SMTP)
- Observability: Actuator health + Prometheus metrics, Grafana dashboard

### Frontend
- Next.js app for landing, feed, communities, posts, profiles, alerts, and admin
- Sticky header navigation and site footer
- **One-click light / dark theme** toggle (preference saved in `localStorage`, respects system preference on first visit)
- Toast notifications and confirm dialogs (no browser `alert`/`confirm`)
- JWT stored client-side; all API traffic goes through the gateway (`NEXT_PUBLIC_API_URL`)

## Architecture

```
Browser → frontend:3000
       → api-gateway:8080
            ├─ user-auth-service:8081      (auth_db)
            ├─ community-service:8082      (community_db)
            ├─ post-service:8083           (post_db)
            ├─ comment-service:8084        (comment_db)
            ├─ vote-service:8085           (vote_db)
            ├─ media-service:8086          (media_db)
            ├─ audit-service:8087          (audit_db)
            └─ notification-service:8088   (notification_db)

Postgres · pgAdmin · Prometheus · Grafana
```

Services talk over HTTP (Spring WebClient). The gateway validates JWT and injects identity headers (`X-User-Id`, `X-User-Roles`, `X-Request-Id`). Cross-service score/karma and internal fan-out use an **internal API key** (never exposed to the browser).

More detail: [docs/architecture.md](docs/architecture.md), [docs/notifications-audit-follows.md](docs/notifications-audit-follows.md).

## Quick start (Docker)

**Prerequisites:** Docker Desktop.

```bash
cp .env.example .env
docker compose up --build
```

Prod-like (host publishes only frontend `:3000` + gateway `:8080`; Swagger off; Compose ≥ 2.24):

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

See [docs/security.md](docs/security.md) and [docs/deploy.md](docs/deploy.md) for CORS, secrets, bootstrap, and actuator guidance.

| Surface | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API gateway | http://localhost:8080 |
| Auth Swagger | http://localhost:8081/swagger-ui.html (local debug only; disabled under `prod`) |
| pgAdmin | http://localhost:5050 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 (`admin` / `admin` by default) |

First start can take several minutes while Maven downloads dependencies inside image builds.

Bootstrap admin (if free): username `admin` / password from `BOOTSTRAP_ADMIN_*` in `.env`. Prod-like Compose sets `BOOTSTRAP_ADMIN_ENABLED=false` — enable once on an empty DB if needed, then disable.

## Frontend routes

| Route | Purpose |
|---|---|
| `/` | Landing overview |
| `/feed` | Post feed (hot / new / top) |
| `/communities` | List / create communities |
| `/c/{slug}` | Community page |
| `/post/{id}` | Post detail + comment tree |
| `/submit` | Create post (optional media) |
| `/u/{username}` | Profile, follow, posts |
| `/notifications` | Alerts inbox |
| `/login`, `/register` | Auth |
| `/forgot-password`, `/reset-password` | Password reset |
| `/admin` | Role management, reports, audit search (admins) |

Theme toggle sits in the **header** (moon / sun). Choice is stored as `threadly-theme` in the browser.

## Demo script (≈3 minutes)

1. Open http://localhost:3000 → **Sign up** (or log in as admin)
2. Optionally flip **Dark / Light** in the header
3. **Communities** → create e.g. `programming` → **Join**
4. **New post** → publish a thread (try an image/file if you like)
5. Open the post → comment → reply (nested tree)
6. Upvote / downvote; open your profile for karma
7. Follow another user (second account) → check **Alerts** when they post
8. As admin: **Admin** → roles, platform report, audit search
9. Grafana → *Threadly Microservices Overview*

Optional seed (stack must be running):

```bash
bash scripts/seed-demo.sh
```

## Repository layout

```
services/
  api-gateway/
  user-auth-service/
  community-service/
  post-service/
  comment-service/
  vote-service/
  media-service/
  audit-service/
  notification-service/
  frontend/                 # Next.js + TypeScript
infra/
  postgres/init/            # DB bootstrap SQL
  prometheus/
  grafana/
docs/                       # architecture, API, security, deploy, …
scripts/                    # seed-demo, run-tests
legacy/                     # historical monolith reference (not used by Compose)
.env.example
docker-compose.yml
docker-compose.prod.yml      # prod-like port surface + prod profile
```

## Configuration

Copy `.env.example` → `.env`. Important variables:

| Area | Keys |
|---|---|
| Postgres | `POSTGRES_USER`, `POSTGRES_PASSWORD` |
| JWT | `JWT_SECRET` (≥ 32 chars), `JWT_EXPIRATION_MS` |
| Internal calls | `INTERNAL_API_KEY` |
| CORS | `CORS_ALLOWED_ORIGINS` (gateway + community WS) |
| Rate limits | `RATE_LIMIT_*` |
| Bootstrap admin | `BOOTSTRAP_ADMIN_*` |
| Media caps | `MEDIA_MAX_*` |
| Mail / reset | `MAIL_ENABLED`, `MAIL_*`, `FRONTEND_BASE_URL` |
| Frontend API | `NEXT_PUBLIC_API_URL` (default `http://localhost:8080`) |

**Never commit real production secrets.** `.env` should stay local / CI-only secrets.

## Unit tests (before every push)

Local CI-style gate (no cloud required):

```powershell
.\scripts\run-tests.ps1
```

```bash
./scripts/run-tests.sh
```

Only commit when green.

## Local development (without full Compose)

1. Start data plane: `docker compose up postgres pgadmin -d`
2. Ensure DBs exist (see `infra/postgres/init/`). If you have an older volume, you may need:

   ```sql
   CREATE DATABASE audit_db;
   CREATE DATABASE notification_db;
   ```

3. Run a service: `cd services/user-auth-service && mvn spring-boot:run`
4. Frontend: `cd services/frontend && npm install && npm run dev`

Point services at local Postgres hosts/DBs as in `.env.example`.

## Documentation

| Doc | Contents |
|---|---|
| [docs/architecture.md](docs/architecture.md) | Request path, votes/karma, observability |
| [docs/api-overview.md](docs/api-overview.md) | HTTP API map |
| [docs/security.md](docs/security.md) | JWT, RBAC, rate limits, prod surface / CORS / actuator |
| [docs/media-storage.md](docs/media-storage.md) | Upload limits and storage |
| [docs/notifications-audit-follows.md](docs/notifications-audit-follows.md) | Follows, inbox, audit, reports |
| [docs/coding-conventions.md](docs/coding-conventions.md) | Layered style (controller → service → ports) |
| [docs/deploy.md](docs/deploy.md) | Local vs prod-like Compose, free VM, optional Vercel |
| [docs/production-readiness-checklist.md](docs/production-readiness-checklist.md) | Living prod hardening checklist |
| [docs/redis.md](docs/redis.md) | Redis-backed gateway rate limits |
| [docs/kubernetes.md](docs/kubernetes.md) | Local Kind/K8s + Ingress load balancing |
| [docs/edge-tls.md](docs/edge-tls.md) | Cloudflare / Nginx TLS + WAF runbook |
| [docs/optional-services-and-urls.md](docs/optional-services-and-urls.md) | Toggle CAPTCHA / OAuth / SMTP; change public URLs after deploy |
| [docs/resume-bullets.md](docs/resume-bullets.md) | Resume-ready project bullets |

## Security notes

- **JWT Bearer** on protected APIs; public GETs for feeds/communities where designed. See [docs/security.md](docs/security.md).
- **RBAC** enforced in APIs from token claims; admins manage roles in UI.
- Edge gateway strips forged identity / internal keys; domain services re-check JWT where required.
- **Rate limiting** on gateway and services (stricter bucket for login/signup).
- Score/karma/notifications: local write + remote compensation / fan-out (documented consistency tradeoffs).
- Structured logs with `requestId` / `userId` in MDC where configured.
- Secrets via `.env` only — rotate `JWT_SECRET` and `INTERNAL_API_KEY` in production.

## Tech stack

| Layer | Stack |
|---|---|
| Frontend | Next.js 14, React 18, TypeScript |
| Gateway | Spring Cloud Gateway |
| Services | Spring Boot, Spring Security, WebClient, Flyway, JPA |
| Data | PostgreSQL 16 |
| Ops | Docker Compose, Redis, Kubernetes manifests, GitHub Actions CI, Prometheus, Grafana, pgAdmin |

## License / status

Personal / portfolio microservices project. Extend freely; keep secrets out of git.
