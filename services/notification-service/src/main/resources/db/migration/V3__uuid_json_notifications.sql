CREATE EXTENSION IF NOT EXISTS pgcrypto;

DROP TABLE IF EXISTS notifications CASCADE;

CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient       JSONB        NOT NULL,
    user_id         UUID         NOT NULL,
    type            VARCHAR(50)  NOT NULL,
    title           VARCHAR(300) NOT NULL,
    body            VARCHAR(1000),
    summary         VARCHAR(500),
    link_url        VARCHAR(1000),
    media_url       VARCHAR(1000),
    actor_username  VARCHAR(50),
    post_title      VARCHAR(300),
    resource_type   VARCHAR(50),
    resource_id     VARCHAR(100),
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notif_user_created ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notif_user_unread ON notifications (user_id) WHERE read_at IS NULL;
