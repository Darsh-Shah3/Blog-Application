# Resume / portfolio bullets (paste after deploy)

**Project name:** Threadly — Reddit-style Blog Platform (Microservices)

**Tech:** Java 17, Spring Boot 3, Spring Cloud Gateway, PostgreSQL, Flyway, Next.js, TypeScript, Docker Compose, Redis, Kubernetes, GitHub Actions CI, Prometheus, Grafana, JWT, REST, S3-compatible media (MinIO/R2)

## Suggested bullets

- Designed and implemented a Reddit-style blogging platform as **Spring Boot microservices** behind a Spring Cloud API gateway, with **database-per-service PostgreSQL** and **Flyway** schema migrations.
- Built JWT authentication/profiles, community membership, feeds (**hot/new/top**), nested comments, up/down voting with **cross-service score & karma updates** via WebClient HTTP APIs.
- Hardened the public surface so browsers reach **only the frontend + API gateway**; backends stay on a private Docker/K8s network (prod-like Compose profile).
- Added **Redis** for gateway rate limits (multi-replica), JWT logout/blacklist, hot-read caches (feeds/communities/profiles), and notification unread counters.
- Authored **Kubernetes manifests** (Deployments, Services, Ingress) with gateway replicas and Ingress load balancing for local Kind/Docker Desktop demos.
- Implemented **S3-compatible media storage** (`ObjectStorage` port) with local disk default and MinIO/R2/AWS via `MEDIA_STORAGE=s3`.
- Containerized the full stack with **Docker Compose** and exposed production-style **health + Prometheus metrics** on each service.
- Set up a **GitHub Actions CI** pipeline: matrix unit tests, Next.js build, Docker image build, and **GHCR** push on `main` (CD deploy deferred).
- Delivered a **Next.js + TypeScript** client that consumes only the edge gateway for auth, feeds, communities, post threads, and voting.
- Documented architecture, security, Redis, Kubernetes, edge TLS, production readiness checklist, and deploy runbooks.

## Honest positioning

Claim CI / Redis / K8s / S3 on the resume only after you have run them locally (Compose + optional Kind). CD deploy can stay “planned” until you have a host.

## LinkedIn / GitHub one-liner

> Threadly: Reddit-like communities as Java microservices + Next.js, with Redis (rate limits, JWT revoke, caches), K8s manifests, GitHub Actions CI, and Prometheus/Grafana — shipped via Docker Compose.
