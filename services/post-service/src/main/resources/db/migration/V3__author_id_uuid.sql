CREATE EXTENSION IF NOT EXISTS pgcrypto;
-- User references become UUID (align with user-auth-service). Existing bigint values cannot cast — clear domain rows.
TRUNCATE TABLE posts RESTART IDENTITY CASCADE;
ALTER TABLE posts ALTER COLUMN author_id TYPE UUID USING gen_random_uuid();
