-- Agenda items of a mosque (lectures, Friday programme, classes, ...).
-- Only PUBLISHED events are visible outside the admin API; CANCELLED events stay visible with their status
-- so people who planned to come see the cancellation. visibility REGISTERED_USERS hides an event from
-- anonymous visitors.

CREATE TABLE event (
    id                   UUID          NOT NULL,
    mosque_id            UUID          NOT NULL,
    title                VARCHAR(150)  NOT NULL,
    description          VARCHAR(10000),
    category             VARCHAR(30)   NOT NULL,
    starts_at            TIMESTAMPTZ   NOT NULL,
    ends_at              TIMESTAMPTZ,
    location             VARCHAR(200),
    image_url            VARCHAR(500),
    visibility           VARCHAR(20)   NOT NULL,
    registration_enabled BOOLEAN       NOT NULL,
    capacity             INTEGER,
    status               VARCHAR(20)   NOT NULL,
    published_at         TIMESTAMPTZ,
    cancelled_at         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ   NOT NULL,
    created_by           UUID          NOT NULL,
    updated_at           TIMESTAMPTZ   NOT NULL,
    updated_by           UUID          NOT NULL,
    version              BIGINT        NOT NULL,
    CONSTRAINT pk_event PRIMARY KEY (id),
    CONSTRAINT fk_event_mosque_id FOREIGN KEY (mosque_id) REFERENCES mosque (id),
    CONSTRAINT fk_event_created_by FOREIGN KEY (created_by) REFERENCES app_user (id),
    CONSTRAINT fk_event_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id),
    -- lets event_registration reference (id, mosque_id), so a registration can never point across mosques
    CONSTRAINT uk_event_id_mosque_id UNIQUE (id, mosque_id),
    CONSTRAINT ck_event_category CHECK (category IN ('LECTURE', 'FRIDAY_PROGRAM', 'CLASS', 'YOUTH', 'COMMUNITY', 'OTHER')),
    CONSTRAINT ck_event_visibility CHECK (visibility IN ('PUBLIC', 'REGISTERED_USERS')),
    CONSTRAINT ck_event_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED')),
    CONSTRAINT ck_event_period CHECK (ends_at IS NULL OR ends_at > starts_at),
    CONSTRAINT ck_event_capacity CHECK (capacity IS NULL OR capacity > 0),
    CONSTRAINT ck_event_capacity_needs_registration CHECK (capacity IS NULL OR registration_enabled),
    CONSTRAINT ck_event_published_at CHECK (status = 'DRAFT' OR published_at IS NOT NULL)
);

-- Public agenda: published events of one mosque, ordered by start time.
CREATE INDEX ix_event_mosque_status_starts_at ON event (mosque_id, status, starts_at);
