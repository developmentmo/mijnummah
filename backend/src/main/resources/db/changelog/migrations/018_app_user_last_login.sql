--liquibase formatted sql

--changeset mijnummah:018-app-user-last-login
-- When the account last signed in with its password (not on token refresh). Shown to the platform administrator,
-- e.g. to see whether an invited administrator has started (fase B).

ALTER TABLE app_user ADD COLUMN last_login_at TIMESTAMPTZ;
