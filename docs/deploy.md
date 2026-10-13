# Free-tier deployment guide

## Local debug vs prod-like Compose

| Mode | Command | What the host sees |
|---|---|---|
| **Local debug** | `docker compose up --build` | Frontend `:3000`, gateway `:8080`, services `8081–8088`, Postgres/pgAdmin, Prometheus/Grafana |
| **Prod-like** | `docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build` | **Only** frontend `:3000` + gateway `:8080` |

Prod-like override (`docker-compose.prod.yml`):

- Unpublishes internal ports via Compose Spec `ports: !override []` (**requires Docker Compose ≥ 2.24**)
- Sets `SPRING_PROFILES_ACTIVE=prod` on Java services (Swagger off; actuator = health + prometheus; health details never)
- Sets `BOOTSTRAP_ADMIN_ENABLED=false` on auth-service
- Sets `CORS_ALLOWED_ORIGINS` on **api-gateway** and **community-service** (defaults `http://localhost:3000,http://127.0.0.1:3000`)

Backends, Postgres, and ops UIs remain reachable **on the Docker network** only (e.g. Prometheus scrape, `docker compose exec`).

Redis is included in the base Compose stack for gateway rate limits, JWT blacklist, and hot-read caches ([redis.md](./redis.md)).  
Kubernetes local demos: [kubernetes.md](./kubernetes.md). TLS/WAF: [edge-tls.md](./edge-tls.md).  
More hardening detail: [security.md](./security.md). Checklist: [production-readiness-checklist.md](./production-readiness-checklist.md).

## Option A — Full stack on one free VM (recommended for Grafana + multi-service)

**Oracle Cloud Always Free (ARM)** or any free/cheap Linux VPS:

1. Open TCP ports: `3000` (frontend), `8080` (API). Prefer a reverse proxy on `80`/`443` later. Do **not** publish `8081–8088`, Postgres, or Grafana to the internet when using prod-like Compose.
2. Install Docker + Docker Compose plugin (**≥ 2.24** for `!override`).
3. Clone the repo:
   ```bash
   git clone https://github.com/Darsh-Shah3/Blog-Application.git
   cd Blog-Application
   cp .env.example .env
   ```
4. Edit `.env`:
   - Strong `JWT_SECRET`, `POSTGRES_PASSWORD`, `INTERNAL_API_KEY` (replace all `CHANGE_ME_*`)
   - `CORS_ALLOWED_ORIGINS=http://YOUR_PUBLIC_IP:3000` (or your HTTPS frontend origin; comma-separated if multiple)
   - `NEXT_PUBLIC_API_URL=http://YOUR_PUBLIC_IP:8080` (or HTTPS reverse proxy URL)
   - `BOOTSTRAP_ADMIN_ENABLED=false` for normal runs (see bootstrap order below)
   - Optional CAPTCHA / Google OAuth / SMTP: see [optional-services-and-urls.md](./optional-services-and-urls.md)
5. Launch (prod-like):
   ```bash
   docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
   ```
   For a full local debug surface on the same machine, use `docker compose up -d --build` instead.
6. (Optional) Put Nginx + Let's Encrypt in front of gateway/frontend (true 80/443).

### Bootstrap admin order (empty auth DB)

1. Temporarily set `BOOTSTRAP_ADMIN_ENABLED=true` (in `.env` or a one-off compose override).
2. Start (or recreate) `user-auth-service` so the platform admin is created.
3. Log in, change the password, then set `BOOTSTRAP_ADMIN_ENABLED=false` and restart auth.
4. Prod-like Compose already defaults bootstrap to **false** — only flip it on for that first empty-DB boot.

### Memory tips

- JVM services use `-Xmx256m` by default.
- On very small VMs, temporarily comment out Prometheus/Grafana in Compose and keep screenshots from local runs for the resume.

## Option B — Frontend on Vercel, APIs on VM

1. Deploy backend Compose stack without the `frontend` service (prefer prod-like override so only `:8080` is public).
2. Create a Vercel project from `services/frontend`.
3. Set env `NEXT_PUBLIC_API_URL=https://api.yourdomain.com`.
4. Set `CORS_ALLOWED_ORIGINS` to your Vercel origin(s) (gateway + community WebSocket share the same env name).

## Health checks

Gateway (always published):

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:3000
```

Under **prod-like** Compose, host ports `8081–8088` are **not** published. Check a domain service from inside the network, for example:

```bash
docker compose exec api-gateway wget -qO- http://user-auth-service:8081/actuator/health
```

Under **local debug** Compose only, you can also curl `http://localhost:8081/actuator/health` (and siblings) from the host.

## Seed demo data

```bash
export API=http://localhost:8080
bash scripts/seed-demo.sh
```
