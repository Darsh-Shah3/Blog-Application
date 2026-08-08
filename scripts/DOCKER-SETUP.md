# Docker path — Threadly (no local JDK / Maven / Node)

## Prerequisites

1. **Docker Desktop** installed and **running** (whale icon in system tray)
2. Project root has `.env` (yours already does)

## Start everything

From the project root (`Blog-Application`):

```powershell
cd "C:\Users\hp\OneDrive\Desktop\Blog Application\Blog-Application"
docker compose up -d --build
```

First build can take **10–20+ minutes** (Maven + Next.js image builds).

## Watch progress

```powershell
docker compose ps
docker compose logs -f api-gateway
```

When services are healthy you should see `threadly-gateway`, `threadly-frontend`, `threadly-auth`, etc. as **running**.

## Open the app

| What | URL |
|------|-----|
| **Website** | http://localhost:3000 |
| **API gateway** | http://localhost:8080 |
| **Auth health** | http://localhost:8081/actuator/health |
| **pgAdmin** | http://localhost:5050 (admin@threadly.local / admin_secret) |
| **Grafana** | http://localhost:3001 (admin / admin) |

## Login defaults

- Bootstrap admin (if enabled): **admin** / **Admin@12345**
- Or register a new user from the site

## Database (handled by Docker)

Postgres runs in container `threadly-postgres`:

| | |
|--|--|
| Host (from your PC / pgAdmin) | `localhost:5433` |
| Host (from other containers) | `postgres:5432` |

> **Port note:** Host port is **5433** so it does not clash with a local Windows PostgreSQL install on **5432**.  
> If desktop pgAdmin uses `localhost:5432`, you are looking at the **wrong** (empty) server — use **5433**.
| User | `threadly` |
| Password | `threadly_secret` |

Databases are created by `infra/postgres/init/01-create-databases.sql` **only on first volume create**.  
Tables are created by **Flyway** when each service starts.

### If services fail with “database does not exist”

Old volume may predate audit/notification DBs. Create them once:

```powershell
docker exec -i threadly-postgres psql -U threadly -d postgres -c "CREATE DATABASE audit_db;"
docker exec -i threadly-postgres psql -U threadly -d postgres -c "CREATE DATABASE notification_db;"
# (ignore "already exists" errors)
docker compose restart audit-service notification-service
```

Or full reset (wipes all Docker DB data):

```powershell
docker compose down -v
docker compose up -d --build
```

## Useful commands

```powershell
# Status
docker compose ps

# Logs for one service
docker compose logs -f user-auth-service

# Restart one service after code change
docker compose up -d --build user-auth-service

# Stop stack (keep data)
docker compose down

# Stop and delete volumes (fresh DB)
docker compose down -v
```

## Smoke test path

1. Open http://localhost:3000  
2. Sign up / log in  
3. Communities → create → New post  
4. Follow another user → see **Alerts** after they post  
5. Admin (admin user) → report + audit events  
6. Admin → **Analytics** (`/admin/analytics`) for live Prometheus graphs (request rate, p95, 5xx, heap)  
7. Optional ops UI: Grafana http://localhost:3001 (`admin`/`admin`) — see `docs/observability.md`  
