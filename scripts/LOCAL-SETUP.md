# Local run checklist (no full Docker stack)

## Database credentials (project defaults)

| Setting | Value |
|--------|--------|
| Host | `localhost` |
| Port | `5432` |
| Username | `threadly` |
| Password | `threadly_secret` |

These match root `.env`:

```env
POSTGRES_USER=threadly
POSTGRES_PASSWORD=threadly_secret
```

Services also expect JDBC:

```text
jdbc:postgresql://localhost:5432/<db_name>
```

User / password above for all DBs.

## Databases (one per service)

| Database | Service | Port |
|----------|---------|------|
| `auth_db` | user-auth-service | 8081 |
| `community_db` | community-service | 8082 |
| `post_db` | post-service | 8083 |
| `comment_db` | comment-service | 8084 |
| `vote_db` | vote-service | 8085 |
| `media_db` | media-service | 8086 |
| `audit_db` | audit-service | 8087 |
| `notification_db` | notification-service | 8088 |

You only create **empty databases + role**.  
**Tables = Flyway** on first service start (do not recreate schema SQL by hand).

## Step 1 — Install PostgreSQL

Install Postgres 16+ locally and ensure it is running on port **5432**.

## Step 2 — Create role + databases

**Option A — psql (recommended, re-runnable):**

```powershell
psql -U postgres -h localhost -f "scripts/setup-local-databases.sql"
```

**Option B — simple (pgAdmin / one-shot):**

Open `scripts/setup-local-databases-simple.sql` as superuser on database `postgres` and execute.  
Ignore “already exists” errors if you re-run.

Verify:

```powershell
psql -U threadly -h localhost -d auth_db -c "\conninfo"
```

Password when prompted: `threadly_secret`

## Step 3 — Env for local JVM services

Your root `.env` already has secrets. For **IDE / Maven** (not Docker), services default to:

- `POSTGRES_HOST=localhost` (set if needed)
- `POSTGRES_USER=threadly`
- `POSTGRES_PASSWORD=threadly_secret`
- `JWT_SECRET` same on **every** service
- `INTERNAL_API_KEY` same on every service
- `MAIL_*` for forgot-password + follow emails

PowerShell example before starting a service:

```powershell
$env:POSTGRES_HOST="localhost"
$env:POSTGRES_PORT="5432"
$env:POSTGRES_USER="threadly"
$env:POSTGRES_PASSWORD="threadly_secret"
$env:JWT_SECRET="ThR3@dly-Jwt-Hs256-Secret-Key-2026-Industry-Grade-9f3c8a2b7e1d"
$env:INTERNAL_API_KEY="threadly-internal-svc-key-9f3c8a2b"
$env:MAIL_ENABLED="true"
# ... other MAIL_* from .env
```

Or map them in IntelliJ/VS Code “Run Configuration” env vars / `application-local.yml`.

## Step 4 — Start services locally (order matters)

Need: **JDK 17+**, **Maven**, **Node 20+** (frontend).

In separate terminals (from each `services/*` folder, or multi-module):

1. `user-auth-service` (8081) — needs `POSTGRES_DB=auth_db`
2. `community-service` (8082) — `community_db`
3. `post-service` (8083) — `post_db` + URLs to auth/community/audit/notification
4. `comment-service` (8084)
5. `vote-service` (8085)
6. `media-service` (8086)
7. `audit-service` (8087)
8. `notification-service` (8088)
9. `api-gateway` (8080) — points to all of the above
10. Frontend: `cd services/frontend && npm install && npm run dev` → http://localhost:3000  
    Set `NEXT_PUBLIC_API_URL=http://localhost:8080`

Per service example:

```powershell
cd "services\user-auth-service"
$env:POSTGRES_HOST="localhost"
$env:POSTGRES_DB="auth_db"
$env:POSTGRES_USER="threadly"
$env:POSTGRES_PASSWORD="threadly_secret"
$env:JWT_SECRET="ThR3@dly-Jwt-Hs256-Secret-Key-2026-Industry-Grade-9f3c8a2b7e1d"
$env:INTERNAL_API_KEY="threadly-internal-svc-key-9f3c8a2b"
mvn spring-boot:run
```

Health check:

```text
http://localhost:8081/actuator/health
http://localhost:8080/actuator/health
```

## Step 5 — Smoke test

1. Open http://localhost:3000 → landing page  
2. Sign up / log in (or bootstrap **admin** / `Admin@12345`)  
3. Create community → post → follow flow → notifications  
4. Admin → report + audit  

Mail: with `MAIL_ENABLED=true`, forgot-password and follow-post emails use Gmail SMTP from `.env`.

## Step 6 — Docker later

When local checks pass:

```powershell
docker compose up -d --build
```

Compose uses the **same** credentials from `.env`.  
Postgres init creates databases only on a **new** volume; for existing volumes run the SQL scripts once against the container.

## Common issues

| Symptom | Fix |
|---------|-----|
| Auth failed / password | Role is `threadly` / `threadly_secret` |
| Database does not exist | Re-run setup SQL |
| Connection refused | Postgres not running / wrong port |
| JWT invalid between services | All must share `JWT_SECRET` |
| Tables missing | Service never started successfully; check Flyway logs |
| Gateway 401 | Call APIs through :8080 with `Authorization: Bearer …` after login |
