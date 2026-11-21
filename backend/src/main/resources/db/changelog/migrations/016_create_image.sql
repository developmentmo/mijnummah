--liquibase formatted sql

--changeset mijnummah:016-create-image
-- Uploaded images (fase A): logo and cover photo of a mosque, later images of events and announcements. The file
-- itself is in the image store (a directory, see mijnummah.storage); this table records which mosque it belongs to,
-- so one mosque can never use another mosque's image. Images are re-encoded on upload, which also removes metadata
-- such as GPS coordinates.

CREATE TABLE image (
    id            UUID         NOT NULL,
    mosque_id     UUID         NOT NULL,
    content_type  VARCHAR(20)  NOT NULL,
    width         INTEGER      NOT NULL,
    height        INTEGER      NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    uploaded_by   UUID,
    created_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_image PRIMARY KEY (id),
    CONSTRAINT fk_image_mosque_id FOREIGN KEY (mosque_id) REFERENCES mosque (id) ON DELETE CASCADE,
    CONSTRAINT fk_image_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES app_user (id) ON DELETE SET NULL,
    CONSTRAINT ck_image_content_type CHECK (content_type IN ('image/jpeg', 'image/png')),
    CONSTRAINT ck_image_dimensions CHECK (width > 0 AND height > 0 AND size_bytes > 0)
);

CREATE INDEX ix_image_mosque_id ON image (mosque_id);

ALTER TABLE mosque ADD COLUMN logo_image_id UUID;
ALTER TABLE mosque ADD COLUMN cover_image_id UUID;
ALTER TABLE mosque
    ADD CONSTRAINT fk_mosque_logo_image_id FOREIGN KEY (logo_image_id) REFERENCES image (id) ON DELETE SET NULL;
ALTER TABLE mosque
    ADD CONSTRAINT fk_mosque_cover_image_id FOREIGN KEY (cover_image_id) REFERENCES image (id) ON DELETE SET NULL;
