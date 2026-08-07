-- Database-per-service isolation on a single Postgres instance
CREATE DATABASE auth_db;
CREATE DATABASE community_db;
CREATE DATABASE post_db;
CREATE DATABASE comment_db;
CREATE DATABASE vote_db;
CREATE DATABASE media_db;
CREATE DATABASE audit_db;
CREATE DATABASE notification_db;

GRANT ALL PRIVILEGES ON DATABASE auth_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE community_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE post_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE comment_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE vote_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE media_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE audit_db TO threadly;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO threadly;
