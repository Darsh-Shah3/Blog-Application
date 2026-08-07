## API overview (via gateway :8080)

All public traffic goes through the gateway. Path design is resource-oriented and explicit.

```http
Authorization: Bearer <jwt>
```

**JWT is required for every business API** except:

- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`

Internal paths (`/api/v1/internal/**`) are **not** routed through the gateway; they require `X-Internal-Api-Key` on the service network only.

Full security / RBAC: [security.md](security.md).

## Auth & profiles (`user-auth-service`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/signup` | no | Create account (ROLE_USER) |
| POST | `/api/v1/auth/login` | no | Login → JWT |
| POST | `/api/v1/auth/forgot-password` | no | Email a reset link (SMTP) |
| POST | `/api/v1/auth/reset-password` | no | Set new password with token |
| GET | `/api/v1/auth/session` | yes | Current session user |
| PUT | `/api/v1/auth/profile` | yes | Update own bio / display name |
| GET | `/api/v1/profiles/{username}` | yes | Public profile by username |
| GET | `/api/v1/profiles/by-id/{id}` | yes | Public profile by id |
| GET | `/api/v1/roles` | yes | Role catalog |
| GET | `/api/v1/admin/users` | ADMIN | List / search users |
| PUT | `/api/v1/admin/users/{id}/roles` | ADMIN | Replace roles |
| GET | `/api/v1/admin/roles` | ADMIN | Role catalog (admin) |

Bootstrap admin (dev): `admin` / `Admin@12345`.

### Signup body
```json
{ "username": "darsh", "email": "darsh@example.com", "password": "secret12", "displayName": "Darsh" }
```

### Internal (service mesh only)

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/internal/users/{id}` | User for enrichment |
| POST | `/api/v1/internal/users/{id}/karma-delta` | Apply karma delta |

## Communities

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/communities` | List / search (`q`) |
| POST | `/api/v1/communities` | Create |
| GET | `/api/v1/communities/{id}` | By id |
| GET | `/api/v1/communities/by-slug/{slug}` | By slug |
| POST | `/api/v1/communities/{id}/join` | Join |
| POST | `/api/v1/communities/{id}/leave` | Leave |
| DELETE | `/api/v1/communities/{id}` | Creator or mod/admin |
| GET | `/api/v1/communities/{id}/members` | Member user ids |
| GET | `/api/v1/communities/memberships/user/{userId}` | Joined community ids |

## Posts

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/posts?sort=hot\|new\|top` | Feed |
| GET | `/api/v1/posts?communityId=` | Community feed |
| GET | `/api/v1/posts?authorId=` | Author feed |
| GET | `/api/v1/posts?q=` | Title search |
| POST | `/api/v1/posts` | Create |
| GET | `/api/v1/posts/{id}` | Detail |
| DELETE | `/api/v1/posts/{id}` | Own or mod/admin |

### Internal posts

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/internal/posts/{id}/score-delta` | Score delta |
| POST | `/api/v1/internal/posts/{id}/comment-count-delta` | Comment count delta |

## Comments

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/comments` | Create (`postId`, optional `parentId`) |
| GET | `/api/v1/comments/threads/{postId}` | Nested thread tree |
| GET | `/api/v1/comments/{id}` | Single comment |
| DELETE | `/api/v1/comments/{id}` | Own or mod/admin |

### Internal comments

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/internal/comments/{id}/score-delta` | Score delta |

## Votes

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/votes` | Cast `value` -1, 0, or 1 |
| GET | `/api/v1/votes/current?targetType=POST&targetId=` | Caller's vote |

```json
{ "targetType": "POST", "targetId": 1, "value": 1 }
```

Cross-service vote flow uses a **local transaction + compensating remote deltas**: if score/karma remote steps fail, prior remote writes are reversed and the vote row is rolled back.

## Media

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/media/uploads` | Multipart `file` (image / video / zip / pdf / …) |
| GET | `/api/v1/media/{id}` | Metadata (`kind`, size, url) |
| GET | `/api/v1/media/{id}/content` | Binary stream |

Posts store only `mediaId`. File bytes stay on the media-service volume. See [media-storage.md](media-storage.md).

**Default size caps:** image 5 MB · video 50 MB · document/zip 20 MB · other 10 MB · absolute 50 MB.

## Local tests (no cloud CI required)

```powershell
# PowerShell
.\scripts\run-tests.ps1

# Bash
./scripts/run-tests.sh
```

Only push when all service unit tests pass.
