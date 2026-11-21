--liquibase formatted sql

--changeset mijnummah:014-mosque-status
-- A mosque has a status instead of an active flag (fase A).
--   ACTIVE    visible in the app; its administrators manage it
--   INACTIVE  hidden from the app, e.g. while it is being set up; its administrators can prepare content
--   BLOCKED   temporarily blocked by the platform: hidden from the app, and only platform administrators can
--             manage it
-- Nothing is deleted in any status.

ALTER TABLE mosque ADD COLUMN status VARCHAR(20);

UPDATE mosque SET status = CASE WHEN active THEN 'ACTIVE' ELSE 'INACTIVE' END;

ALTER TABLE mosque ALTER COLUMN status SET NOT NULL;
ALTER TABLE mosque ADD CONSTRAINT ck_mosque_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'));
ALTER TABLE mosque DROP COLUMN active;

CREATE INDEX ix_mosque_status_name ON mosque (status, name);
