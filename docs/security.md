# Security: JWT, rate limiting, logging

## Production hardening (public surface)

Browsers should reach **only** the frontend (`:3000`) and API gateway (`:8080`). Domain services, Postgres, pgAdmin, Prometheus, and Grafana stay on the Docker network.

| Mode | Command | Host ports |
|---|---|---|
| Local debug | `docker compose up --build` | Frontend, gateway, 8081–8088, Postgres, tools |
| Prod-like | `docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build` | Frontend `:3000` + gateway `:8080` only |

Requires **Docker Compose ≥ 2.24** (`ports: !override`). Prod-like sets `SPRING_PROFILES_ACTIVE=prod` and `BOOTSTRAP_ADMIN_ENABLED=false`.

### CORS

`CORS_ALLOWED_ORIGINS` (comma-separated) drives gateway `CorsConfig` and community WebSocket origins. Defaults: `http://localhost:3000,http://127.0.0.1:3000`. Set to your real frontend origin(s) for deploy.

### Swagger & Actuator (prod profile)

- `application-prod.yml` on all Java apps: SpringDoc **disabled**; actuator exposes `health,prometheus` only; health `show-details: never`
- Gateway `PublicRoutes`: only `/actuator/health` is unauthenticated at the edge (not prometheus/info)
- Domain `SecurityConfig`: permits health + prometheus (for in-network scrape), not full `/actuator/**`
- Gateway `SwaggerRedirectConfig` is `@Profile("!prod")`

### Secrets (prod-like / public)

Use strong unique values in `.env` (never commit real secrets):

- `JWT_SECRET` (≥ 32 chars), `INTERNAL_API_KEY`, `POSTGRES_PASSWORD`
- First admin on an empty auth DB: temporarily `BOOTSTRAP_ADMIN_ENABLED=true`, create the admin, then set **false** again (compose prod override defaults to false)

Redis (rate limits, JWT blacklist, caches): [redis.md](./redis.md).  
See [deploy.md](./deploy.md) and [production-readiness-checklist.md](./production-readiness-checklist.md).

## Roles & rights (RBAC)

Threadly uses **role-based access control**. Roles live in `auth_db` and are embedded in JWT:

| Claim | Contents |
|---|---|
| `roles` | e.g. `ROLE_USER`, `ROLE_MODERATOR`, `ROLE_ADMIN` |
| `permissions` | derived rights such as `CONTENT_DELETE_ANY`, `ROLE_ASSIGN` |

Gateway validates JWT and injects `X-User-Id`, `X-Username`, `X-User-Roles`, `X-User-Permissions` (client-forged headers are stripped).

### Seeded roles

| Role | Who | Rights |
|---|---|---|
| **ROLE_USER** | Default on register | Create communities/posts/comments, vote, delete **own** content |
| **ROLE_MODERATOR** | Assigned by admin | All USER rights + delete **any** post/comment + delete communities |
| **ROLE_ADMIN** | Bootstrap + assigned | Full catalog including `USER_MANAGE` / `ROLE_ASSIGN` |

### Bootstrap admin (first start)

If `BOOTSTRAP_ADMIN_ENABLED=true` (local `.env` default), auth-service creates:

- username: `admin` (override with `BOOTSTRAP_ADMIN_USERNAME`)
- password: from `BOOTSTRAP_ADMIN_PASSWORD` (local Compose may still use a demo default — override in `.env`)
- roles: `ROLE_USER` + `ROLE_ADMIN`

Prod-like Compose forces `BOOTSTRAP_ADMIN_ENABLED=false`. On a fresh empty auth DB, enable it once to create the platform admin, then disable again. Always use a strong password and never leave the demo credential on a public host.

### Admin APIs (require `ROLE_ADMIN`)

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/admin/users?q=` | List / search **active** users |
| PUT | `/api/v1/admin/users/{id}/roles` | Replace roles set |
| DELETE | `/api/v1/admin/users/{id}` | Soft-delete user |
| GET | `/api/v1/admin/roles` | Role catalog + permissions |
| GET | `/api/v1/roles` | Same catalog for any authenticated client |

After role changes, outstanding JWTs are **revoked in Redis** (`bl:user:{id}`); the target user must **log in again** so a new JWT is issued with updated claims.

### Password reset (SMTP)

1. `POST /api/v1/auth/forgot-password` `{ "email": "..." }` → always generic success message.
2. Token stored hashed in `password_reset_tokens` (30 min default).
3. Email sent when `MAIL_ENABLED=true` + `MAIL_HOST` set; otherwise the **reset link is logged** in auth-service (dev).
4. User opens `/reset-password?token=...` and calls `POST /api/v1/auth/reset-password`.

Env keys: `MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `FRONTEND_BASE_URL`.

### Content moderation

- Delete post/comment: author **or** `CONTENT_DELETE_ANY` (moderator/admin)
- Delete community: creator **or** `COMMUNITY_DELETE` / admin

## JWT Bearer authentication

- Algorithm: **HS256**
- Shared secret env: `JWT_SECRET` (all services + gateway must match)
- Client header: `Authorization: Bearer <accessToken>`
- Token issued by `user-auth-service` on signup/login (includes `jti`)
- **Logout:** `POST /api/v1/auth/logout` blacklists the current `jti` in Redis (TTL = remaining life)
- **Revoke-all:** account soft-delete / admin delete / role change sets `bl:user:{userId}`
- Gateway + community STOMP reject blacklisted tokens; Redis errors **fail open** (site stays up)

Redis-backed gateway rate limits + caches: [redis.md](./redis.md).

### Public endpoints (no JWT)
- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`
- `OPTIONS /*` (CORS preflight)
- `GET /actuator/health` at the gateway (prometheus stays on the Docker network for scrape)

### All other business APIs require JWT

Gateway validates the signature, strips client-forged `X-User-Id` / `X-Internal-Api-Key`, and injects trusted identity headers.

Each domain service also validates JWT (or service internal API key) before handling the request.

### Inter-service calls

Service-to-service calls use header:

```http
X-Internal-Api-Key: <INTERNAL_API_KEY>
```

This key is **stripped** at the public gateway so browsers cannot forge internal privileges.

## Distributed consistency (microservices)

Each service owns its Postgres database. Full two-phase commit across services is not used.

Instead:

1. **Local `@Transactional`** on every multi-step write inside one service (vote row, comment row, community + membership).
2. **Hard-fail remote calls** for score / karma / comment-count (no silent swallow).
3. **Compensating deltas** when a later remote step fails (e.g. reverse post score if karma fails).
4. Throwing after compensation causes the **local DB transaction to roll back** so vote/comment rows are not left inconsistent with partial side effects.

See `VoteServiceImpl` and `CommentServiceImpl` / `WebClientPostAdapter`.

## Local test gate (no cloud CI)

```powershell
.\scripts\run-tests.ps1
```

Commit and push only when every service suite is green.

| Layer | Scope | Default |
|---|---|---|
| API gateway | Per client IP | 120 req/min (20/min for login+signup) |
| Each Spring service | Per client IP | 180 req/min (120 for auth-service) |

Exceeded limit → HTTP **429** + `Retry-After: 60` (gateway) and headers:

- `X-RateLimit-Limit`
- `X-RateLimit-Remaining`

Toggle via `RATE_LIMIT_ENABLED=true|false`.

## Logging

Every service uses the same console pattern (SLF4J + Logback):

```text
yyyy-MM-dd HH:mm:ss.SSS LEVEL [service=name] [api=METHOD /path] [Class.method] [user=username|id=42] [req=uuid] - message
```

| Field | Source |
|---|---|
| timestamp | Log event time |
| LEVEL | `DEBUG` / `INFO` / `WARN` / `ERROR` |
| service | `spring.application.name` of the microservice |
| api | HTTP method + path (MDC) |
| Class.method | Java logger class + calling method |
| user / id | JWT `username` + user id when authenticated |
| req | `X-Request-Id` correlation id |

| Level | Used for |
|---|---|
| **DEBUG** | JWT accepted, inter-service fetches, detailed flow |
| **INFO** | Request start/end, login/signup success, business actions |
| **WARN** | 4xx, invalid JWT, rate limit hits, failed logins |
| **ERROR** | 5xx and unexpected failures |

Correlation: `X-Request-Id` is generated at the gateway (or preserved), propagated, and attached to MDC.

## Audit columns (`created_by` / `updated_by`)

Content tables store the **username** of who created and last updated the row (`posts`, `comments`, `communities`, `media_files`, `votes`, `users`). Internal side-effects (score/karma deltas) set `updated_by` to `system`.

## User soft-delete

Users are **soft-deleted** (`deleted_at` timestamp), not removed from the table.

| Endpoint | Who |
|---|---|
| `DELETE /api/v1/auth/account` | Signed-in user (self) |
| `DELETE /api/v1/admin/users/{id}` | Admin |

After soft-delete:

- Login / password-reset / public profile resolve only **active** rows
- **Username and email uniqueness apply only while `deleted_at IS NULL`** (Postgres partial unique indexes), so the same username/email can register again

## Example authenticated call

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"emailOrUsername":"demo","password":"demo1234"}' | jq -r .accessToken)

curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/posts?sort=hot
```
