-- Deleting an account (fase 5, AVG) removes the person's event registrations as well, which also frees their
-- places. Accounts that administer a mosque or authored content cannot be deleted (those foreign keys stay
-- restrictive); the application reports that instead.

ALTER TABLE event_registration DROP CONSTRAINT fk_event_registration_user_id;
ALTER TABLE event_registration
    ADD CONSTRAINT fk_event_registration_user_id FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE;
