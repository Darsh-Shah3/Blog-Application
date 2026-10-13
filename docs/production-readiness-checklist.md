# Production readiness checklist

Living checklist for deploying **Threadly** as a public, user-safe website (including **CI/CD**).  
Update checkboxes as items are completed. Related docs: [security.md](./security.md), [deploy.md](./deploy.md), [media-storage.md](./media-storage.md), [observability.md](./observability.md), [architecture.md](./architecture.md), [resume-bullets.md](./resume-bullets.md).

**How to use**

- `[ ]` = not done · `[x]` = done · `[~]` = partial / needs revisit
- Keep **Status** notes short (date + owner optional)
- Re-run this list before every production release

| Meta | Value |
|------|--------|
| Last reviewed | 2026-10-13 |
| Target environment | _staging / production_ |
| Reviewer | _name_ |

---

## 1. Secrets & configuration

| Done | Item | Notes / status |
|------|------|----------------|
| [ ] | No production secrets in git (only `.env.example` / secret manager) | |
| [~] | Strong unique `JWT_SECRET` (≥ 32 chars; not Compose default) | Docs + `.env.example` guidance; operator must set real values |
| [~] | Strong unique `POSTGRES_PASSWORD` (not `threadly_secret`) | Same — template uses `CHANGE_ME_*` |
| [~] | Strong unique `INTERNAL_API_KEY` (not default) | Same — operator must set |
| [ ] | `JWT_EXPIRATION_MS` set appropriately for prod (consider shorter than 24h) | |
| [~] | `BOOTSTRAP_ADMIN_ENABLED=false` after first admin exists (or never on prod) | Prod Compose defaults false; enable once on empty DB then disable |
| [ ] | Bootstrap / admin password changed from `Admin@12345` | |
| [ ] | `FRONTEND_BASE_URL` and `NEXT_PUBLIC_API_URL` use HTTPS public URLs | |
| [x] | CORS allowlist limited to real frontend origin(s) | Env-driven `CORS_ALLOWED_ORIGINS` on gateway + community WS |
| [ ] | Mail / OAuth / reCAPTCHA secrets set only if those features are enabled | |
| [ ] | Secrets rotated documented (who/when, after leak or staff change) | |

---

## 2. Network, TLS & edge protection

| Done | Item | Notes / status |
|------|------|----------------|
| [ ] | HTTPS on frontend and API (TLS cert; HTTP redirects to HTTPS) | Runbook: [edge-tls.md](./edge-tls.md) |
| [~] | Only ports 80/443 public; Postgres / Redis / Actuator / Grafana not exposed | Prod Compose: `:3000` + `:8080` only; lock to 80/443 at edge |
| [x] | Reverse proxy or cloud load balancer in front of gateway + frontend | Docs + K8s Ingress; operator still wires Nginx/Cloudflare |
| [x] | CDN / WAF / DDoS (e.g. Cloudflare) in front of public origins | Documented in edge-tls.md |
| [ ] | HSTS enabled at edge | |
| [ ] | WebSocket (community chat / DM) works through TLS proxy | Nginx headers documented |

---

## 3. Authentication & authorization

| Done | Item | Notes / status |
|------|------|----------------|
| [x] | JWT validation at gateway; forged identity headers stripped | Already in gateway |
| [x] | RBAC enforced in services (`ROLE_USER` / `MODERATOR` / `ADMIN`) | See security.md |
| [x] | Internal routes require `X-Internal-Api-Key` | |
| [ ] | Stricter rate limits on login / register / password-reset verified in prod config | |
| [ ] | Password reset email works end-to-end (`MAIL_ENABLED=true` + SMTP) | |
| [ ] | Account lockout / CAPTCHA on auth abuse paths (optional but recommended) | |
| [x] | Token logout / refresh strategy decided (blacklist or short-lived + refresh) | Redis JWT blacklist + `POST /auth/logout`; see [redis.md](./redis.md) |
| [ ] | Prefer httpOnly secure cookies for tokens (hardening vs `localStorage`) | Optional upgrade |
| [ ] | Admin UI / APIs restricted; no default admin credentials in prod | |

---

## 4. Application security (code & APIs)

| Done | Item | Notes / status |
|------|------|----------------|
| [x] | Rate limiting on gateway and services | Gateway Redis-backed (+ in-memory fallback); services keep local filters |
| [x] | Media type blocking + size caps | See media-storage.md |
| [x] | Swagger / OpenAPI disabled or auth-protected in production | `SPRING_PROFILES_ACTIVE=prod` disables springdoc (`application-prod.yml`) |
| [x] | Actuator endpoints restricted (health public OK; metrics/env private) | Prod: expose health+prometheus; edge public = health only; details never |
| [ ] | Security headers on frontend (CSP, X-Frame-Options, Referrer-Policy, etc.) | |
| [ ] | XSS-safe rendering of user content (posts, comments, chat) | |
| [ ] | CSRF strategy reviewed if moving to cookie-based auth | |
| [ ] | Dependency vulnerabilities scanned (Maven + npm) and criticals fixed | |
| [ ] | Container images scanned; base images kept updated | |
| [ ] | No stack traces / internal paths in public API error bodies | |

---

## 5. Data, backups & storage

| Done | Item | Notes / status |
|------|------|----------------|
| [ ] | Managed Postgres (or HA self-hosted) for production | |
| [ ] | Automated DB backups + restore tested at least once | |
| [ ] | Backup retention policy defined (e.g. 7 daily / 4 weekly) | |
| [x] | Media not only on a single Docker volume — object storage (S3/R2/GCS) or backup plan | `MEDIA_STORAGE=s3` + MinIO profile / R2; see media-storage.md |
| [ ] | DB users least-privilege (app user ≠ superuser) | |
| [ ] | Encryption at rest enabled on DB / object storage | |
| [ ] | PII / deletion policy understood (soft-delete vs hard-delete, GDPR-style requests) | |

---

## 6. Redis (recommended for production scale)

| Done | Item | Notes / status |
|------|------|----------------|
| [x] | Redis deployed (managed preferred) | Compose `redis` service; see [redis.md](./redis.md) |
| [x] | Gateway rate limits backed by Redis (shared across replicas) | `RATE_LIMIT_REDIS_ENABLED` + `RateLimitGlobalFilter` |
| [x] | Auth-sensitive buckets (login/signup/reset) use Redis | Same Redis keys, stricter RPM |
| [x] | Optional: cache hot reads (feeds / communities / profiles) | `CACHE_REDIS_ENABLED` on post/community/auth |
| [x] | Optional: token blacklist / session revoke in Redis | `TOKEN_BLACKLIST_ENABLED` + `jti` / `bl:user:*` |
| [x] | Notification unread counters in Redis | `notif:unread:v1:*` with DB fallback |
| [x] | Redis not publicly reachable; AUTH password / ACL set | Prod Compose unpublishes port; `--requirepass` |
| [x] | Redis persistence / eviction policy chosen intentionally | AOF (`--appendonly yes`) |

---

## 7. Kubernetes, replicas & load balancing

| Done | Item | Notes / status |
|------|------|----------------|
| [x] | Deployment strategy chosen (K8s / managed containers / hardened Compose) | Compose + `infra/k8s` (local Kind); see [kubernetes.md](./kubernetes.md) |
| [x] | Manifests or IaC for services, frontend, gateway | `infra/k8s/*.yaml` + kustomization |
| [~] | Load balancer / Ingress routes HTTPS → frontend + API | Ingress HTTP paths ready; TLS at edge in Phase 4 docs |
| [x] | ≥ 2 replicas for gateway (and critical services if needed) | `gateway.yaml` replicas: 2 |
| [x] | Liveness / readiness probes on `/actuator/health` | |
| [x] | Resource requests/limits set (CPU/memory) | |
| [ ] | Rolling updates / zero-downtime deploy verified | |
| [ ] | Horizontal Pod Autoscaler (optional, after baseline load known) | |
| [x] | Secrets via K8s Secrets / cloud secret manager (not plain env files on disk) | `secret.example.yaml` template |
| [ ] | Network policies: services cannot reach DB/Redis from public | |

---

## 8. Observability & operations

| Done | Item | Notes / status |
|------|------|----------------|
| [x] | Actuator health + Prometheus metrics on services | Local Compose |
| [x] | Grafana dashboard available | Profile `tools` |
| [ ] | Prometheus / Grafana not public; auth on dashboards | |
| [ ] | Centralized logs (or at least log shipping) with `requestId` / `userId` | |
| [ ] | Alerts on 5xx rate, auth failures, disk, DB down, cert expiry | |
| [ ] | Uptime check on frontend + `/actuator/health` from outside | |
| [ ] | Runbook: restart, rollback, rotate secrets, restore backup | |
| [ ] | Staging environment mirrors production config (minus real user data) | |

---

## 9. Frontend & UX production

| Done | Item | Notes / status |
|------|------|----------------|
| [ ] | Production Next.js build (`next build`) used — not `dev` | |
| [ ] | `NEXT_PUBLIC_API_URL` points at public HTTPS API | |
| [ ] | Error pages and toast failures do not leak internals | |
| [ ] | Upload UX respects media size limits with clear errors | |
| [ ] | Mobile layout smoke-tested | |
| [ ] | Analytics / privacy notice if you collect usage data (optional) | |

---

## 10. Quality gates before each release

| Done | Item | Notes / status |
|------|------|----------------|
| [ ] | `./scripts/run-tests.sh` or `.\scripts\run-tests.ps1` green | |
| [ ] | Smoke test: register, login, create community, post, comment, vote | |
| [ ] | Smoke test: media upload + view | |
| [ ] | Smoke test: password reset (if mail enabled) | |
| [ ] | Smoke test: admin soft-delete / role assign (admin only) | |
| [ ] | Chat / DM basic send-receive through prod URLs | |
| [ ] | Load sanity check (even light): rate limit returns 429 under abuse | |
| [ ] | Changelog / release notes for this deploy | |

---

## 11. CI/CD pipeline (resume + production)

CI is in [`.github/workflows/ci.yml`](../.github/workflows/ci.yml). **CD deploy is intentionally deferred** until you pick a host.

| Done | Item | Notes / status |
|------|------|----------------|
| [x] | Choose CI platform (GitHub Actions recommended) | `.github/workflows/ci.yml` |
| [x] | CI on every PR / push to `main`: backend unit tests (`mvn test` per service or aggregate script) | Matrix over all Java services |
| [x] | CI: frontend lint + `npm test` / `npm run build` (Next.js) | lint soft-fail; build required |
| [ ] | CI: fail the job if tests fail (branch protection: require green checks) | Enable in GitHub branch settings |
| [x] | Build & tag Docker images in CI (gateway + all services + frontend) | |
| [x] | Push images to a registry (GHCR / Docker Hub / ECR) | Push to GHCR on `main`/`master` only |
| [x] | Cache Maven / npm deps in CI for faster pipelines | setup-java / setup-node + GHA docker cache |
| [x] | Separate **CI** (test + build) from **CD** (deploy) jobs/workflows | No deploy job yet |
| [ ] | CD: auto-deploy to **staging** on merge to `main` (or `develop`) | Deferred |
| [ ] | CD: **production** deploy via manual approval / tagged release | Deferred |
| [ ] | Deploy method documented (Compose SSH, K8s `kubectl`/`helm`, or cloud deploy action) | |
| [x] | Pipeline secrets in CI secret store (JWT, DB, registry token) — never in YAML | Uses `GITHUB_TOKEN` for GHCR |
| [ ] | Image vulnerability scan step (e.g. Trivy) on built images | |
| [ ] | Optional: SAST / dependency audit (`npm audit`, OWASP Dependency-Check) | |
| [x] | Status badge in README (build passing) | |
| [ ] | Pipeline runbook: how to re-run, rollback a bad deploy, rotate CI secrets | |

**Resume-friendly wording (CI done; CD later):**

> Implemented a **CI pipeline** (GitHub Actions) that runs automated microservice tests, builds Docker images, and pushes to GHCR. Production CD deploy is planned next.

---

## 12. Legal / user safety (public site)

| Done | Item | Notes / status |
|------|------|----------------|
| [ ] | Terms of use / community guidelines linked | |
| [ ] | Privacy policy (what you store, cookies, email) | |
| [ ] | Report / moderation path works for admins | Audit/reports exist — verify UX |
| [ ] | Contact / abuse email published | |
| [ ] | Age / content expectations clear if needed for your audience | |

---

## 13. Nice-to-have (after core prod is stable)

| Done | Item | Notes / status |
|------|------|----------------|
| [ ] | Message queue (Kafka / Rabbit) for vote/karma / notification fan-out | Architecture allows later |
| [ ] | Multi-region or warm standby | |
| [ ] | Canary / blue-green deploys | |
| [ ] | Formal penetration test | |
| [ ] | Bug bounty or responsible-disclosure page | |
| [ ] | SIEM / advanced intrusion detection | |

---

## Suggested completion order

Use this sequence so the site is safe before it is fancy:

1. **Secrets + HTTPS + lock admin + hide Swagger/Actuator** (sections 1–4 essentials)
2. **CI pipeline** — tests + Docker build on every PR (section 11; do this early for resume + safety)
3. **Backups + media storage plan** (section 5)
4. **CDN/WAF edge** (section 2)
5. **Redis rate limits** (section 6)
6. **Replicas + load balancer / K8s** (section 7)
7. **CD deploy** to staging/prod from the same pipeline (section 11)
8. **Alerts + staging + runbooks** (section 8)
9. **Legal pages + full smoke suite** (sections 10 & 12)
10. **Nice-to-haves** (section 13)

---

## Change log

| Date | Change |
|------|--------|
| 2026-10-13 | Initial production readiness checklist created |
| 2026-10-13 | Added section 11: CI/CD pipeline checklist + resume wording |
| 2026-10-13 | Phases 1–4: hardening, Redis, K8s, edge-TLS + S3 storage |
| 2026-10-13 | Phase 5: GitHub Actions CI (tests, Docker build, GHCR push; no CD) |
| 2026-10-13 | Phase 1 hardening: CORS env, Swagger/actuator prod profile, Compose port surface |

_Update this table whenever you add, remove, or re-prioritize checklist items._
