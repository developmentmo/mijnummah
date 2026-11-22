--liquibase formatted sql

--changeset mijnummah:020-central-app
-- Central app (fase D). Only additions; no existing data changes.
--
-- * A followed mosque can be marked as a favourite (shown first in the app).
-- * Users can save videos as favourites.
-- * push_device.mosque_id is no longer filled for new tokens (one app; who is reached follows from what the user
--   follows). The column stays for tokens of the former apps per mosque and is cleared when such a token
--   registers again.

ALTER TABLE mosque_follow ADD COLUMN favorite BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE media_favorite (
    user_id    UUID        NOT NULL,
    media_id   UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_media_favorite PRIMARY KEY (user_id, media_id),
    CONSTRAINT fk_media_favorite_user_id FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_media_favorite_media_id FOREIGN KEY (media_id) REFERENCES media_item (id) ON DELETE CASCADE
);

CREATE INDEX ix_media_favorite_media_id ON media_favorite (media_id);
CREATE INDEX ix_mosque_status_city ON mosque (status, city);
