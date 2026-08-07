# Audit, notifications, follows, reports

## Audit event store (`audit-service` :8087, `audit_db`)

Every meaningful **create / update / delete** should be published to:

`POST /api/v1/internal/audit/events` (internal API key)

Admin UI / API:

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/audit/events` | Filter: `service`, `action`, `resourceType`, `actorUsername`, `q`, `from`, `to` |
| GET | `/api/v1/reports/overview` | Platform counts + 7-day action breakdown |
| GET | `/api/v1/reports/activity` | Same activity bundle |

Post-service already publishes CREATE / DELETE for posts. Extend the same client pattern in other services as needed.

## Notification service (`notification-service` :8088, `notification_db`)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/notifications` | `unreadOnly`, `type`, `q`, pageable |
| GET | `/api/v1/notifications/unread-count` | `{ count }` |
| POST | `/api/v1/notifications/{id}/read` | mark one |
| POST | `/api/v1/notifications/read-all` | mark all |
| POST | `/api/v1/internal/notifications/post-created` | fan-out to followers |

When a post is created, notification-service loads followers from auth (`GET /api/v1/internal/users/{id}/followers`), creates inbox rows (`FOLLOW_NEW_POST`), and emails when `MAIL_ENABLED=true`.

## Follow users (auth-service)

| Method | Path |
|---|---|
| POST | `/api/v1/profiles/{username}/follow` |
| DELETE | `/api/v1/profiles/{username}/follow` |
| GET | `/api/v1/profiles/{username}/follow-status` |
| GET | `/api/v1/profiles/{username}/followers` |
| GET | `/api/v1/profiles/{username}/following` |

## Frontend

| Route | Purpose |
|---|---|
| `/` | Marketing landing (what you can do) + site footer |
| `/feed` | Home post feed + title search + hot/new/top |
| `/notifications` | Inbox + filters |
| `/u/{username}` | Profile + Follow button |
| `/admin` | Roles + platform report + audit search |

## Existing DBs upgrade

If Postgres volume already exists, create DBs once:

```sql
CREATE DATABASE audit_db;
CREATE DATABASE notification_db;
GRANT ALL PRIVILEGES ON DATABASE audit_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO threadly;
```
