CREATE EXTENSION IF NOT EXISTS pgcrypto;
-- User references become UUID. Clear existing rows (bigint author_id cannot map to auth UUIDs).
TRUNCATE TABLE comments RESTART IDENTITY CASCADE;
ALTER TABLE comments ALTER COLUMN author_id TYPE UUID USING gen_random_uuid();
