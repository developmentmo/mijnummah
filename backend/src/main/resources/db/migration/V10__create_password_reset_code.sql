-- One-time codes for resetting a forgotten password (fase 5). The code is sent by email and typed into the app.
-- Only a hash is stored. A code is valid for a short time, for a limited number of attempts, and only the newest
-- unused code of a user counts (requesting a new code invalidates older ones).

CREATE TABLE password_reset_code (
    id             UUID        NOT NULL,
    user_id        UUID        NOT NULL,
    code_hash      VARCHAR(64) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,
    attempts       INTEGER     NOT NULL,
    used_at        TIMESTAMPTZ,
    invalidated_at TIMESTAMPTZ,
    version        BIGINT      NOT NULL,
    CONSTRAINT pk_password_reset_code PRIMARY KEY (id),
    CONSTRAINT fk_password_reset_code_user_id FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT ck_password_reset_code_attempts CHECK (attempts >= 0),
    CONSTRAINT ck_password_reset_code_expiry CHECK (expires_at > created_at)
);

CREATE INDEX ix_password_reset_code_user_id ON password_reset_code (user_id, created_at DESC);
