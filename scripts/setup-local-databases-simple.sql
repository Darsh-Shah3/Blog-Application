-- =============================================================================
-- Threadly — simple local setup (works in pgAdmin / DBeaver “execute as script”)
-- Prefer scripts/setup-local-databases.sql when using psql (idempotent \gexec).
-- =============================================================================
-- Credentials after run:
--   User:     threadly
--   Password: threadly_secret
--   Host:     localhost
--   Port:     5432
-- =============================================================================

-- Run connected to database "postgres" as a superuser (often "postgres").

CREATE USER threadly WITH PASSWORD 'threadly_secret';
-- If user exists, ignore the error and continue.

CREATE DATABASE auth_db OWNER threadly;
CREATE DATABASE community_db OWNER threadly;
CREATE DATABASE post_db OWNER threadly;
CREATE DATABASE comment_db OWNER threadly;
CREATE DATABASE vote_db OWNER threadly;
CREATE DATABASE media_db OWNER threadly;
CREATE DATABASE audit_db OWNER threadly;
CREATE DATABASE notification_db OWNER threadly;

GRANT ALL PRIVILEGES ON DATABASE auth_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE community_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE post_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE comment_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE vote_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE media_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE audit_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO threadly;
