-- UUID primary keys + denormalized join columns + JSON follow graph + reset rate limits.
-- Existing BIGINT data is rebuilt onto UUID keys (local/dev safe; remaps active users).

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- -------------------------------------------------------------------------
-- roles → UUID
-- -------------------------------------------------------------------------
CREATE TABLE roles_new (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(255)
);

INSERT INTO roles_new (id, name, description)
SELECT gen_random_uuid(), name, description FROM roles;

CREATE TABLE role_id_map AS
SELECT r.id AS old_id, n.id AS new_id
FROM roles r
JOIN roles_new n ON n.name = r.name;

-- -------------------------------------------------------------------------
-- users → UUID
-- -------------------------------------------------------------------------
CREATE TABLE users_new (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(50)  NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    bio             VARCHAR(500),
    karma           BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMPTZ
);

INSERT INTO users_new (
    id, username, email, password_hash, display_name, bio, karma,
    created_at, updated_at, created_by, updated_by, deleted_at
)
SELECT gen_random_uuid(), username, email, password_hash, display_name, bio, karma,
       created_at, updated_at, created_by, updated_by, deleted_at
FROM users;

CREATE TABLE user_id_map AS
SELECT u.id AS old_id, n.id AS new_id
FROM users u
JOIN users_new n ON n.username = u.username AND n.email = u.email;

CREATE UNIQUE INDEX uq_users_username_active_new ON users_new (username) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_users_email_active_new ON users_new (email) WHERE deleted_at IS NULL;

-- -------------------------------------------------------------------------
-- user_roles: own UUID PK + denormalized username / role_name
-- -------------------------------------------------------------------------
CREATE TABLE user_roles_new (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users_new (id) ON DELETE CASCADE,
    role_id    UUID NOT NULL REFERENCES roles_new (id) ON DELETE CASCADE,
    username   VARCHAR(50)  NOT NULL,
    role_name  VARCHAR(50)  NOT NULL,
    UNIQUE (user_id, role_id)
);

INSERT INTO user_roles_new (id, user_id, role_id, username, role_name)
SELECT gen_random_uuid(),
       um.new_id,
       rm.new_id,
       un.username,
       rn.name
FROM user_roles ur
JOIN user_id_map um ON um.old_id = ur.user_id
JOIN role_id_map rm ON rm.old_id = ur.role_id
JOIN users_new un ON un.id = um.new_id
JOIN roles_new rn ON rn.id = rm.new_id;

-- -------------------------------------------------------------------------
-- password_reset_tokens → UUID + username + used flag
-- -------------------------------------------------------------------------
CREATE TABLE password_reset_tokens_new (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users_new (id) ON DELETE CASCADE,
    username        VARCHAR(50)  NOT NULL,
    token_hash      VARCHAR(128) NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ  NOT NULL,
    used            BOOLEAN      NOT NULL DEFAULT FALSE,
    used_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

INSERT INTO password_reset_tokens_new (
    id, user_id, username, token_hash, expires_at, used, used_at, created_at
)
SELECT gen_random_uuid(),
       um.new_id,
       un.username,
       prt.token_hash,
       prt.expires_at,
       (prt.used_at IS NOT NULL),
       prt.used_at,
       prt.created_at
FROM password_reset_tokens prt
JOIN user_id_map um ON um.old_id = prt.user_id
JOIN users_new un ON un.id = um.new_id;

CREATE INDEX idx_password_reset_user_new ON password_reset_tokens_new (user_id);
CREATE INDEX idx_password_reset_user_created_new ON password_reset_tokens_new (user_id, created_at DESC);

-- Per-user forgot-password rate limit (3 / calendar day, 1h lock after 3)
CREATE TABLE password_reset_rate_limits (
    user_id              UUID PRIMARY KEY REFERENCES users_new (id) ON DELETE CASCADE,
    username             VARCHAR(50)  NOT NULL,
    window_date          DATE         NOT NULL,
    attempt_count        INT          NOT NULL DEFAULT 0,
    locked_until         TIMESTAMPTZ,
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- -------------------------------------------------------------------------
-- user_follows → one row per user, JSON arrays of {id, username}
-- -------------------------------------------------------------------------
CREATE TABLE user_follows_new (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL UNIQUE REFERENCES users_new (id) ON DELETE CASCADE,
    username    VARCHAR(50)  NOT NULL,
    followers   JSONB        NOT NULL DEFAULT '[]'::jsonb,
    following   JSONB        NOT NULL DEFAULT '[]'::jsonb,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Seed a social row for every user
INSERT INTO user_follows_new (id, user_id, username, followers, following, updated_at)
SELECT gen_random_uuid(), un.id, un.username, '[]'::jsonb, '[]'::jsonb, NOW()
FROM users_new un;

-- Rebuild edges into JSON (if any existed)
UPDATE user_follows_new uf
SET following = sub.arr,
    updated_at = NOW()
FROM (
    SELECT um_follower.new_id AS user_id,
           COALESCE(jsonb_agg(jsonb_build_object(
               'id', um_following.new_id::text,
               'username', un_following.username
           ) ORDER BY f.created_at), '[]'::jsonb) AS arr
    FROM user_follows f
    JOIN user_id_map um_follower ON um_follower.old_id = f.follower_id
    JOIN user_id_map um_following ON um_following.old_id = f.following_id
    JOIN users_new un_following ON un_following.id = um_following.new_id
    GROUP BY um_follower.new_id
) sub
WHERE uf.user_id = sub.user_id;

UPDATE user_follows_new uf
SET followers = sub.arr,
    updated_at = NOW()
FROM (
    SELECT um_following.new_id AS user_id,
           COALESCE(jsonb_agg(jsonb_build_object(
               'id', um_follower.new_id::text,
               'username', un_follower.username
           ) ORDER BY f.created_at), '[]'::jsonb) AS arr
    FROM user_follows f
    JOIN user_id_map um_following ON um_following.old_id = f.following_id
    JOIN user_id_map um_follower ON um_follower.old_id = f.follower_id
    JOIN users_new un_follower ON un_follower.id = um_follower.new_id
    GROUP BY um_following.new_id
) sub
WHERE uf.user_id = sub.user_id;

-- -------------------------------------------------------------------------
-- Swap tables
-- -------------------------------------------------------------------------
DROP TABLE IF EXISTS user_follows CASCADE;
DROP TABLE IF EXISTS password_reset_tokens CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS roles CASCADE;

ALTER TABLE roles_new RENAME TO roles;
ALTER TABLE users_new RENAME TO users;
ALTER TABLE user_roles_new RENAME TO user_roles;
ALTER TABLE password_reset_tokens_new RENAME TO password_reset_tokens;
ALTER TABLE user_follows_new RENAME TO user_follows;

ALTER INDEX IF EXISTS uq_users_username_active_new RENAME TO uq_users_username_active;
ALTER INDEX IF EXISTS uq_users_email_active_new RENAME TO uq_users_email_active;
ALTER INDEX IF EXISTS idx_password_reset_user_new RENAME TO idx_password_reset_user;
ALTER INDEX IF EXISTS idx_password_reset_user_created_new RENAME TO idx_password_reset_user_created;

DROP TABLE IF EXISTS role_id_map;
DROP TABLE IF EXISTS user_id_map;
