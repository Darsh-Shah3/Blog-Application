-- =============================================================================
-- Threadly — local PostgreSQL bootstrap (run once before first local start)
-- =============================================================================
-- Default credentials (same as .env / .env.example):
--   Host:     localhost
--   Port:     5432
--   User:     threadly
--   Password: threadly_secret
--
-- How to run (pick one):
--
--   A) psql as superuser (Windows / Linux / Mac):
--        psql -U postgres -h localhost -f scripts/setup-local-databases.sql
--
--   B) pgAdmin / DBeaver: open this file, connect as postgres, execute.
--
--   C) Docker Postgres already running (threadly-postgres):
--        docker exec -i threadly-postgres psql -U threadly -d postgres < scripts/setup-local-databases.sql
--        (If threadly user does not exist yet, connect as the container's superuser — usually
--         POSTGRES_USER=threadly on a fresh compose install, then only CREATE DATABASE parts matter.)
--
-- Tables are NOT in this script — each microservice creates its own schema via Flyway
-- on first start (auth_db → V1/V2…, post_db → V1/V2, etc.).
-- =============================================================================

-- 1) Application login (skip if you already use another superuser for local dev)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'threadly') THEN
        CREATE ROLE threadly LOGIN PASSWORD 'threadly_secret';
        RAISE NOTICE 'Created role threadly';
    ELSE
        -- Keep password in sync with project defaults (optional)
        ALTER ROLE threadly WITH LOGIN PASSWORD 'threadly_secret';
        RAISE NOTICE 'Role threadly already exists — password reset to threadly_secret';
    END IF;
END
$$;

-- 2) One database per microservice
SELECT 'CREATE DATABASE auth_db OWNER threadly'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'auth_db')\gexec

SELECT 'CREATE DATABASE community_db OWNER threadly'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'community_db')\gexec

SELECT 'CREATE DATABASE post_db OWNER threadly'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'post_db')\gexec

SELECT 'CREATE DATABASE comment_db OWNER threadly'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'comment_db')\gexec

SELECT 'CREATE DATABASE vote_db OWNER threadly'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'vote_db')\gexec

SELECT 'CREATE DATABASE media_db OWNER threadly'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'media_db')\gexec

SELECT 'CREATE DATABASE audit_db OWNER threadly'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'audit_db')\gexec

SELECT 'CREATE DATABASE notification_db OWNER threadly'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notification_db')\gexec

-- 3) Grants (safe to re-run)
GRANT ALL PRIVILEGES ON DATABASE auth_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE community_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE post_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE comment_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE vote_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE media_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE audit_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO threadly;

-- 4) Optional sanity check
SELECT datname AS database_name
FROM pg_database
WHERE datname LIKE '%_db'
ORDER BY 1;
