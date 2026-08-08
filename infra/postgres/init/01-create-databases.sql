-- Database-per-service isolation (runs once on first Postgres volume create)
-- Superuser for Docker image is POSTGRES_USER (default: threadly)

SELECT 'CREATE DATABASE auth_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'auth_db')\gexec
SELECT 'CREATE DATABASE community_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'community_db')\gexec
SELECT 'CREATE DATABASE post_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'post_db')\gexec
SELECT 'CREATE DATABASE comment_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'comment_db')\gexec
SELECT 'CREATE DATABASE vote_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'vote_db')\gexec
SELECT 'CREATE DATABASE media_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'media_db')\gexec
SELECT 'CREATE DATABASE audit_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'audit_db')\gexec
SELECT 'CREATE DATABASE notification_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notification_db')\gexec

GRANT ALL PRIVILEGES ON DATABASE auth_db TO CURRENT_USER;
GRANT ALL PRIVILEGES ON DATABASE community_db TO CURRENT_USER;
GRANT ALL PRIVILEGES ON DATABASE post_db TO CURRENT_USER;
GRANT ALL PRIVILEGES ON DATABASE comment_db TO CURRENT_USER;
GRANT ALL PRIVILEGES ON DATABASE vote_db TO CURRENT_USER;
GRANT ALL PRIVILEGES ON DATABASE media_db TO CURRENT_USER;
GRANT ALL PRIVILEGES ON DATABASE audit_db TO CURRENT_USER;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO CURRENT_USER;
