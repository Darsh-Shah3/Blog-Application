# Free-tier deployment guide

## Option A — Full stack on one free VM (recommended for Grafana + multi-service)

**Oracle Cloud Always Free (ARM)** or any free/cheap Linux VPS:

1. Open TCP ports: `3000` (frontend), `8080` (API), optionally `3001` (Grafana), `9090` (Prometheus).
2. Install Docker + Docker Compose plugin.
3. Clone the repo:
   ```bash
   git clone https://github.com/Darsh-Shah3/Blog-Application.git
   cd Blog-Application
   cp .env.example .env
   ```
4. Edit `.env`:
   - Strong `JWT_SECRET`, `POSTGRES_PASSWORD`, `INTERNAL_API_KEY`
   - `NEXT_PUBLIC_API_URL=http://YOUR_PUBLIC_IP:8080` (or HTTPS reverse proxy URL)
5. Launch:
   ```bash
   docker compose up -d --build
   ```
6. (Optional) Put Nginx + Let's Encrypt in front of gateway/frontend.

### Memory tips

- JVM services use `-Xmx256m` by default.
- On very small VMs, temporarily comment out Prometheus/Grafana in Compose and keep screenshots from local runs for the resume.

## Option B — Frontend on Vercel, APIs on VM

1. Deploy backend Compose stack without the `frontend` service.
2. Create a Vercel project from `services/frontend`.
3. Set env `NEXT_PUBLIC_API_URL=https://api.yourdomain.com`.
4. Ensure gateway CORS allows your Vercel origin (extend `globalcors` in api-gateway config).

## Health checks

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:3000
```

## Seed demo data

```bash
export API=http://localhost:8080
bash scripts/seed-demo.sh
```
