-- Short news items of a mosque. Publishing an announcement does NOT send a push notification; sending is a
-- separate, explicit action (fase 5). Expired announcements (expires_at in the past) are hidden publicly.

CREATE TABLE announcement (
    id           UUID          NOT NULL,
    mosque_id    UUID          NOT NULL,
    title        VARCHAR(150)  NOT NULL,
    body         VARCHAR(5000) NOT NULL,
    featured     BOOLEAN       NOT NULL,
    status       VARCHAR(20)   NOT NULL,
    published_at TIMESTAMPTZ,
    expires_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ   NOT NULL,
    created_by   UUID          NOT NULL,
    updated_at   TIMESTAMPTZ   NOT NULL,
    updated_by   UUID          NOT NULL,
    version      BIGINT        NOT NULL,
    CONSTRAINT pk_announcement PRIMARY KEY (id),
    CONSTRAINT fk_announcement_mosque_id FOREIGN KEY (mosque_id) REFERENCES mosque (id),
    CONSTRAINT fk_announcement_created_by FOREIGN KEY (created_by) REFERENCES app_user (id),
    CONSTRAINT fk_announcement_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id),
    CONSTRAINT ck_announcement_status CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT ck_announcement_published_at CHECK (status = 'DRAFT' OR published_at IS NOT NULL)
);

CREATE INDEX ix_announcement_mosque_status_published_at ON announcement (mosque_id, status, published_at DESC);
