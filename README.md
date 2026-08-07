# Threadly

Reddit-style blogging platform rebuilt as **Java Spring Boot microservices** with a **Next.js + TypeScript** frontend, **PostgreSQL** (database-per-service), **Flyway** migrations, **Docker Compose**, **Prometheus**, and **Grafana**.

> Brand name: **Threadly** — communities, posts, nested comments, voting, and karma.

## Architecture

```
Browser → api-gateway:8080
            ├─ user-auth-service:8081   (auth_db)
            ├─ community-service:8082   (community_db)
            ├─ post-service:8083        (post_db)
            ├─ comment-service:8084     (comment_db)
            ├─ vote-service:8085        (vote_db)
            └─ media-service:8086       (media_db)

Postgres · pgAdmin · Prometheus · Grafana · frontend:3000
```

Services communicate over HTTP (Spring WebClient). The gateway validates JWT and injects `X-User-Id` / `X-User-Roles`. Cross-service score and karma updates use an internal API key.

## Quick start (Docker)

Prerequisites: Docker Desktop.

```bash
cp .env.example .env
docker compose up --build
```

| Surface | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API gateway | http://localhost:8080 |
| Auth Swagger | http://localhost:8081/swagger-ui.html |
| pgAdmin | http://localhost:5050 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 (`admin` / `admin`) |

First start can take several minutes while Maven downloads dependencies inside Docker builds.

## Demo script (≈3 minutes)

1. Open http://localhost:3000 → **Sign up**
2. **Communities** → create `programming`
3. **Join** → **New post** → publish a thread
4. Open the post → add a comment → reply (nested tree)
5. Upvote / downvote; check profile karma
6. Open Grafana → *Threadly Microservices Overview* dashboard

Seed helper (optional, needs running stack):

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
  frontend/
infra/
  postgres/init/
  prometheus/
  grafana/
docs/
scripts/                # seed-demo, run-tests (local CI gate)
```

## Unit tests (run before every GitHub push)

You do not need cloud CI — run the local gate and only commit if it is green:

```powershell
.\scripts\run-tests.ps1
```

```bash
./scripts/run-tests.sh
```

## Local development (without full Compose)

1. Start only Postgres: `docker compose up postgres pgadmin -d`
2. Run a single service: `cd services/user-auth-service && mvn spring-boot:run`
3. Frontend: `cd services/frontend && npm install && npm run dev`

Point services at `localhost` Postgres DBs created by `infra/postgres/init/01-create-databases.sql`.

## Free deploy

See [docs/deploy.md](docs/deploy.md) for Oracle Cloud Always Free / single-VM Compose and optional Vercel frontend.

## Resume bullets

See [docs/resume-bullets.md](docs/resume-bullets.md).

## API overview

See [docs/api-overview.md](docs/api-overview.md). Post & file storage: [docs/media-storage.md](docs/media-storage.md).

## Loose coupling & Java style

See [docs/coding-conventions.md](docs/coding-conventions.md) for layered architecture inside each service
(controller → service interface → impl → ports/adapters).

## Security notes

- **JWT Bearer** required on every business API (except signup/login). See [docs/security.md](docs/security.md).
- **RBAC**: `ROLE_USER` (default), `ROLE_MODERATOR` (moderate content), `ROLE_ADMIN` (users + roles). Bootstrap admin: `admin` / `Admin@12345`.
- Cross-service votes/comments: local transactions + compensating remote deltas (no silent partial writes).
- Shared `JWT_SECRET` + edge validation at the gateway; domain services re-validate tokens.
- **Rate limiting** on gateway and every service (per IP). Login/signup use a stricter bucket.
- Structured logs: INFO for traffic, WARN for auth/rate-limit, ERROR for 5xx; `requestId` + `userId` in MDC.
- Secrets live in `.env` (never commit production secrets).
- Clients cannot forge `X-User-Id` or `X-Internal-Api-Key` through the gateway.

## Legacy monolith

Removed. All active code lives under `services/`.
