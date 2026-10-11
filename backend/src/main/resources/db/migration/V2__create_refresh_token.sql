-- Server-side state of opaque refresh tokens. Only the SHA-256 hash is stored, never the token itself.
-- One family per login; every refresh revokes the presented token and links it to its successor.

CREATE TABLE refresh_token (
    id             UUID        NOT NULL,
    user_id        UUID        NOT NULL,
    family_id      UUID        NOT NULL,
    token_hash     VARCHAR(64) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,
    revoked_at     TIMESTAMPTZ,
    replaced_by_id UUID,
    version        BIGINT      NOT NULL,
    CONSTRAINT pk_refresh_token PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_user_id FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_token_replaced_by_id FOREIGN KEY (replaced_by_id) REFERENCES refresh_token (id),
    CONSTRAINT ck_refresh_token_expiry CHECK (expires_at > created_at)
);

CREATE INDEX ix_refresh_token_user_id ON refresh_token (user_id);
CREATE INDEX ix_refresh_token_family_id ON refresh_token (family_id);
