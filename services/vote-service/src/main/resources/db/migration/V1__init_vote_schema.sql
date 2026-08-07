CREATE TABLE votes (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    target_type     VARCHAR(20)  NOT NULL,
    target_id       BIGINT       NOT NULL,
    value           SMALLINT     NOT NULL CHECK (value IN (-1, 1)),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, target_type, target_id)
);

CREATE INDEX idx_votes_target ON votes(target_type, target_id);
