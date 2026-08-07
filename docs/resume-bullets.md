# Resume / portfolio bullets (paste after deploy)

**Project name:** Threadly — Reddit-style Blog Platform (Microservices)

**Tech:** Java 17, Spring Boot 3, Spring Cloud Gateway, PostgreSQL, Flyway, Next.js, TypeScript, Docker Compose, Prometheus, Grafana, JWT, REST

## Suggested bullets

- Designed and implemented a Reddit-style blogging platform as **seven Spring Boot microservices** behind a Spring Cloud API gateway, with **database-per-service PostgreSQL** and **Flyway** schema migrations.
- Built JWT authentication/profiles, community membership, feeds (**hot/new/top**), nested comments, up/down voting with **cross-service score & karma updates** via WebClient HTTP APIs.
- Containerized the full stack with **Docker Compose** (services, Postgres, pgAdmin, Prometheus, Grafana) and exposed production-style **health + Prometheus metrics** on each service.
- Delivered a **Next.js + TypeScript** client that consumes only the edge gateway for auth, feeds, communities, post threads, and voting.
- Documented architecture, API surface, free-tier deploy runbook, and a 3-minute demo path for interviews.

## Honest positioning

Only claim this on the resume once the services build and the demo flow works on your machine (or free VM). Until then, phrase as *in progress* if needed.

## LinkedIn / GitHub one-liner

> Threadly: Reddit-like communities and discussions as Java microservices + Next.js, observed with Prometheus/Grafana, shipped via Docker Compose.
