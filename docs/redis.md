# Redis (rate limits, JWT revoke, caches)

## What Redis is for here

Threadly uses **one Redis** for production edge and hot-path optimization:

| Use | Who writes / reads | Keys (examples) |
|---|---|---|
| **Gateway rate limits** | API gateway | `rl:<minute>:<ip>\|<bucket>` |
| **JWT blacklist** | Auth writes; gateway + community STOMP check | `bl:jti:{jti}`, `bl:user:{userId}` |
| **Hot-read caches** | Post / community / auth services | `post:feed:v1:…`, `post:detail:v1:…`, `community:slug:v1:…`, `user:profile:v1:…` |
| **Unread counters** | Notification service | `notif:unread:v1:{userId}` (+ optional `:{type}`) |

Without Redis, each gateway process keeps rate-limit counters in memory. That works for one local instance, but with **two gateway replicas** each replica has its own counters — an attacker gets ~2× the limit. Shared Redis keys keep limits correct under load balancing.

## How to run (Docker Compose)

Redis is part of the base stack:

```bash
docker compose up -d redis
# or full stack
docker compose up -d --build
```

| Env | Default (Compose) | Notes |
|---|---|---|
| `REDIS_HOST` | `redis` | Service name on the Docker network |
| `REDIS_PORT` | `6379` | |
| `REDIS_PASSWORD` | from `.env` | Required by Compose `redis-server --requirepass` |
| `REDIS_ENABLED` | `true` | Master switch for Redis beans in gateway + domain services |
| `RATE_LIMIT_REDIS_ENABLED` | `true` | Gateway shared rate-limit counters |
| `TOKEN_BLACKLIST_ENABLED` | `true` | Logout / revoke-all + edge/STOMP checks |
| `CACHE_REDIS_ENABLED` | `true` | Feeds, communities, profiles, unread counts |

Prod-like Compose **does not publish** Redis to the host (`docker-compose.prod.yml`).

## Gateway behaviour

| Flag | Behaviour |
|---|---|
| `RATE_LIMIT_REDIS_ENABLED=true` | Shared Redis fixed-window counters (auth paths use a stricter bucket) |
| `TOKEN_BLACKLIST_ENABLED=true` | Reject JWTs listed in `bl:jti:*` or covered by `bl:user:*` |
| Either Redis feature fails | **Fail open** (allow request) + error log so the site stays up |

## JWT logout / revoke

1. Tokens include a `jti` (JWT id).
2. `POST /api/v1/auth/logout` (Bearer required) writes `bl:jti:{jti}` with TTL = remaining token life.
3. Soft-delete account / admin delete / role change writes `bl:user:{userId}` (epoch ms); any token with `iat` ≤ that value is rejected.
4. Frontend logout calls the API best-effort, then clears `localStorage`.
5. Community WebSocket (STOMP) checks the same keys because `/ws/**` is public at the gateway.

## Caches

| Cache | TTL (approx) | Invalidation |
|---|---|---|
| Anonymous / community / author feeds (`hot`/`new`/`top`/`trending`) | 20–90s | Post create/delete/publish |
| Post detail | 90s | Post delete (votes rely on short TTL) |
| Community by slug/id, discover, list | 60s–3m | Create/join/leave/delete |
| Public profiles | 3m | Profile update / soft-delete |
| Notification unread | 30s + INCR/DECR | Create / mark-read / read-all |

Personalized `for_you` feeds and membership-scoped global feeds are **not** cached (high cardinality / per-user).

## What you do **not** need

- No paid Redis Cloud account for local / resume demos
- Domain services keep working if Redis is down (caches miss-through to DB; blacklist/rate-limit fail open)
