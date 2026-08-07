CREATE TABLE communities (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(100) NOT NULL UNIQUE,
    description     VARCHAR(1000),
    creator_id      BIGINT       NOT NULL,
    member_count    BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE community_memberships (
    id              BIGSERIAL PRIMARY KEY,
    community_id    BIGINT NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    user_id         BIGINT NOT NULL,
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (community_id, user_id)
);

CREATE INDEX idx_memberships_user ON community_memberships(user_id);
CREATE INDEX idx_communities_slug ON communities(slug);
