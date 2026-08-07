CREATE TABLE notifications (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    type            VARCHAR(50)  NOT NULL,
    title           VARCHAR(300) NOT NULL,
    body            VARCHAR(1000),
    link_url        VARCHAR(500),
    actor_username  VARCHAR(50),
    resource_type   VARCHAR(50),
    resource_id     VARCHAR(100),
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notif_user_created ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notif_user_unread ON notifications (user_id) WHERE read_at IS NULL;
CREATE INDEX idx_notif_type ON notifications (type);
