CREATE TABLE audit_events (
    id              BIGSERIAL PRIMARY KEY,
    occurred_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    service_name    VARCHAR(80)  NOT NULL,
    action          VARCHAR(20)  NOT NULL,
    resource_type   VARCHAR(50)  NOT NULL,
    resource_id     VARCHAR(100),
    actor_user_id   BIGINT,
    actor_username  VARCHAR(50),
    summary         VARCHAR(500),
    metadata        TEXT,
    request_id      VARCHAR(64)
);

CREATE INDEX idx_audit_occurred ON audit_events (occurred_at DESC);
CREATE INDEX idx_audit_service ON audit_events (service_name, occurred_at DESC);
CREATE INDEX idx_audit_action ON audit_events (action, occurred_at DESC);
CREATE INDEX idx_audit_resource ON audit_events (resource_type, resource_id);
CREATE INDEX idx_audit_actor ON audit_events (actor_username);
CREATE INDEX idx_audit_actor_id ON audit_events (actor_user_id);
