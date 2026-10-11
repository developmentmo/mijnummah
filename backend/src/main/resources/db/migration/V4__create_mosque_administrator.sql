-- Who may manage which mosque. ADMIN: everything for that mosque (details, administrators, registrations,
-- content). EDITOR: content only (events, media, announcements). Rights never extend to other mosques.

CREATE TABLE mosque_administrator (
    id         UUID        NOT NULL,
    mosque_id  UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    role       VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    CONSTRAINT pk_mosque_administrator PRIMARY KEY (id),
    CONSTRAINT uk_mosque_administrator_mosque_user UNIQUE (mosque_id, user_id),
    CONSTRAINT fk_mosque_administrator_mosque_id FOREIGN KEY (mosque_id) REFERENCES mosque (id),
    CONSTRAINT fk_mosque_administrator_user_id FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_mosque_administrator_created_by FOREIGN KEY (created_by) REFERENCES app_user (id),
    CONSTRAINT ck_mosque_administrator_role CHECK (role IN ('ADMIN', 'EDITOR'))
);

CREATE INDEX ix_mosque_administrator_user_id ON mosque_administrator (user_id);
