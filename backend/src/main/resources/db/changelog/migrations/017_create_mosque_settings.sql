--liquibase formatted sql

--changeset mijnummah:017-create-mosque-settings
-- Initial configuration of a mosque, created together with the mosque (fase B). Set by the platform administrator.
--   registrations_enabled  users may register for this mosque's events (each event still has its own switch)
--   push_enabled           this mosque's administrators may send push notifications

CREATE TABLE mosque_settings (
    mosque_id             UUID        PRIMARY KEY REFERENCES mosque (id) ON DELETE CASCADE,
    registrations_enabled BOOLEAN     NOT NULL DEFAULT TRUE,
    push_enabled          BOOLEAN     NOT NULL DEFAULT TRUE,
    updated_at            TIMESTAMPTZ NOT NULL
);

INSERT INTO mosque_settings (mosque_id, updated_at) SELECT id, now() FROM mosque;
