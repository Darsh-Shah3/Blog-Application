# Loose coupling conventions (Java services)

This monorepo follows hexagonal-style layering inside each microservice so features stay swappable.

## Layers

```text
controller  →  service interface  →  service.impl
                      │
                      ├── repository (Spring Data)
                      ├── mapper (entity ↔ DTO)
                      └── port (outbound)  →  client adapters (WebClient)
```

| Layer | Responsibility | Depends on |
|---|---|---|
| **Controller** | HTTP status, validation annotations | Service interface only |
| **Service API** | Business contract (interface) | DTOs / domain types |
| **Service impl** | Use-cases, transactions | ports + repositories |
| **Port** | Outbound contract to another system | Nothing concrete |
| **Adapter** | WebClient / local disk / etc. | Port interface |
| **Config properties** | Typed `app.*` binding | No `@Value` spam |

## Why this style in Java

1. **DI constructor injection** (`@RequiredArgsConstructor`) — testable, final fields.
2. **Interfaces for use-cases** — controllers never import `*Impl`.
3. **Ports for inter-service calls** — domain never imports reactive WebClient types.
4. **`@ConfigurationProperties`** — secrets/URLs change without rewiring classes.
5. **Mappers** — mapping noise out of services.
6. **Comments only on non-obvious bits** — vote flip deltas, tree assembly, JWT edge ordering.
7. **Structured logs** — timestamp, level, service, API method/path, `Class.method`, user, requestId (see `docs/security.md`).
8. **Audit columns** — `created_by` / `updated_by` usernames on domain create/update.
9. **User soft-delete** — `deleted_at` + partial unique indexes for active username/email.
10. **JPQL in `repository/query/*Queries`** — every custom `@Query` string lives in a service query catalog
    (e.g. `AuthQueries`, `PostQueries`); repository interfaces only reference the constants.

## Example flow (create post)

`PostController` → `PostService` → `PostServiceImpl`
  1. `CommunityPort.findById` (HTTP adapter)
  2. `PostRepository.save`
  3. `UserPort.findById` for author username
  4. `PostMapper.toResponse`

Swap `WebClientCommunityAdapter` for a gRPC adapter later: **no service/controller change**.
