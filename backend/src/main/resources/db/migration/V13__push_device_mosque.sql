-- Push tokens per app (fase 7, white-label).
--
-- Every mosque has its own app, and one person can follow several mosques. A push token belongs to one installed
-- app, so it now records the mosque of that app: a notification of mosque B must never arrive in the app of mosque A.
-- Sending only targets devices of the notifying mosque.
--
-- Existing rows: with exactly one mosque (the pilot) they belong to that mosque's app. Otherwise mosque_id stays
-- NULL; such tokens are never targeted and the app registers its token again (with its mosque) on its next start,
-- so nothing is deleted here.

ALTER TABLE push_device ADD COLUMN mosque_id UUID;

UPDATE push_device SET mosque_id = (SELECT id FROM mosque)
WHERE (SELECT count(*) FROM mosque) = 1;

ALTER TABLE push_device
    ADD CONSTRAINT fk_push_device_mosque_id FOREIGN KEY (mosque_id) REFERENCES mosque (id) ON DELETE CASCADE;

CREATE INDEX ix_push_device_mosque_user ON push_device (mosque_id, user_id);
