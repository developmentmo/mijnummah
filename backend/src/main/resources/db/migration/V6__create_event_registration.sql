-- A user's registration for an event. One row per (event, user): cancelling keeps the row with status
-- CANCELLED and registering again re-activates it, so the unique constraint prevents duplicates.

CREATE TABLE event_registration (
    id           UUID        NOT NULL,
    mosque_id    UUID        NOT NULL,
    event_id     UUID        NOT NULL,
    user_id      UUID        NOT NULL,
    status       VARCHAR(20) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    version      BIGINT      NOT NULL,
    CONSTRAINT pk_event_registration PRIMARY KEY (id),
    CONSTRAINT uk_event_registration_event_user UNIQUE (event_id, user_id),
    CONSTRAINT fk_event_registration_event FOREIGN KEY (event_id, mosque_id) REFERENCES event (id, mosque_id),
    CONSTRAINT fk_event_registration_mosque_id FOREIGN KEY (mosque_id) REFERENCES mosque (id),
    CONSTRAINT fk_event_registration_user_id FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT ck_event_registration_status CHECK (status IN ('CONFIRMED', 'CANCELLED'))
);

CREATE INDEX ix_event_registration_user_id ON event_registration (user_id);
