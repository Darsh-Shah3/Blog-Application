# Swagger / API testing (local Docker)

OpenAPI (SpringDoc) is enabled on **each microservice** when **not** running the `prod` profile.  
Use this for **local debug** Compose (`docker compose up`), where ports `8081–8088` are published to the host.

Under **`SPRING_PROFILES_ACTIVE=prod`** (prod-like Compose override), SpringDoc is **disabled** on all nine Java apps (`application-prod.yml`). Swagger UI and `/v3/api-docs` are unavailable in that mode.

The **canonical** API gateway does **not** ship a complete aggregated Swagger UI for day-to-day testing — open each service port directly while debugging. Gateway `SwaggerRedirectConfig` (non-prod only) may redirect `/` to springdoc static assets; that path is incomplete/deferred on the canonical tree and is off under `prod`.

## Swagger UI links (local debug only)

| Service | Port | Swagger UI | OpenAPI JSON |
|---------|------|------------|--------------|
| user-auth | 8081 | http://localhost:8081/swagger-ui/index.html | http://localhost:8081/v3/api-docs |
| community | 8082 | http://localhost:8082/swagger-ui/index.html | http://localhost:8082/v3/api-docs |
| post | 8083 | http://localhost:8083/swagger-ui/index.html | http://localhost:8083/v3/api-docs |
| comment | 8084 | http://localhost:8084/swagger-ui/index.html | http://localhost:8084/v3/api-docs |
| vote | 8085 | http://localhost:8085/swagger-ui/index.html | http://localhost:8085/v3/api-docs |
| media | 8086 | http://localhost:8086/swagger-ui/index.html | http://localhost:8086/v3/api-docs |
| audit | 8087 | http://localhost:8087/swagger-ui/index.html | http://localhost:8087/v3/api-docs |
| notification | 8088 | http://localhost:8088/swagger-ui/index.html | http://localhost:8088/v3/api-docs |

Gateway health: http://localhost:8080/actuator/health  
(Prod-like: only this health URL is public at the edge; service Swagger ports are not published.)

Use **`/swagger-ui/index.html`** (hyphen). Paths like `/webjars/swagger_ui/...` (underscore) **404**.

Gateway `http://localhost:8080/` (non-prod) shows an index of all service Swagger links — the gateway itself does **not** host a full aggregated UI.

## How to test APIs

### 1) Get a JWT (auth service)

1. Open http://localhost:8081/swagger-ui/index.html  
2. Find **`POST /api/v1/auth/login`** (or signup first).  
3. Body example:

```json
{
  "emailOrUsername": "admin",
  "password": "Admin@12345"
}
```

4. Copy `accessToken` from the response.

### 2) Authorize other service UIs

1. Open that service’s Swagger (e.g. posts on 8083).  
2. Click **Authorize**.  
3. Paste the token only (no `Bearer ` prefix if the dialog says “jwt”).  
4. **Try it out** on any endpoint.

Swagger calls **that service port directly** (skips gateway). Each service still validates the JWT.

### 3) Internal APIs (`/api/v1/internal/**`)

These need the header:

```http
X-Internal-Api-Key: threadly-internal-svc-key-9f3c8a2b
```

(value from your `.env` `INTERNAL_API_KEY`).  
In Swagger you can use **Authorize** only for JWT; for internal key, use a tool like curl/Postman or “Try it out” custom headers if shown.

### 4) Via gateway (real client path)

Browser / Postman:

```http
POST http://localhost:8080/api/v1/auth/login
Authorization: Bearer <token>   # on protected routes
```

Public without JWT: signup, login, forgot-password, reset-password.

## Suggested test order

1. **Auth** — signup/login/session/profile  
2. **Community** — create, list, join  
3. **Media** — upload (if testing IMAGE post)  
4. **Post** — create, feed, get by id  
5. **Comment** — thread under post  
6. **Vote** — cast vote / current  
7. **Profiles** — follow / unfollow  
8. **Notification** — list, unread-count  
9. **Audit / reports** — admin JWT (`ROLE_ADMIN`) for `/api/v1/audit/**` and `/api/v1/reports/**`

## Rebuild after OpenAPI config change

If you rebuild Java services for the JWT “Authorize” button on every service:

```powershell
docker compose up -d --build
```

Swagger UI itself already works **without** rebuild; Authorize scheme is nicer after rebuild.
