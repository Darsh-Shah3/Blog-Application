CREATE EXTENSION IF NOT EXISTS pgcrypto;
TRUNCATE TABLE community_memberships RESTART IDENTITY CASCADE;
TRUNCATE TABLE communities RESTART IDENTITY CASCADE;
ALTER TABLE communities ALTER COLUMN creator_id TYPE UUID USING gen_random_uuid();
ALTER TABLE community_memberships ALTER COLUMN user_id TYPE UUID USING gen_random_uuid();
