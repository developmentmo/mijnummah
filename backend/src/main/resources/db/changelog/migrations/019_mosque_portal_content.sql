--liquibase formatted sql

--changeset mijnummah:019-mosque-portal-content
-- Mosque portal (fase C). Only additions and wider checks; no existing data changes.
--
-- * Images for events, video thumbnails and announcements: an uploaded image of the same mosque (table image). The
--   older image_url / thumbnail_url columns stay as a fallback for external links.
-- * Events can be archived: hidden from the app and the public API, kept for the administration.
-- * Videos get the category RECORDED_EVENT; published_at may be set by the administrator (also in the future:
--   the video becomes visible at that moment).
-- * A mosque can link to its prayer times.
-- * Push notifications can be a free message (content_type MESSAGE, no content item) with a chosen audience.

ALTER TABLE event ADD COLUMN image_id UUID;
ALTER TABLE event ADD COLUMN archived_at TIMESTAMPTZ;
ALTER TABLE event ADD CONSTRAINT fk_event_image_id FOREIGN KEY (image_id) REFERENCES image (id) ON DELETE SET NULL;
ALTER TABLE event DROP CONSTRAINT ck_event_status;
ALTER TABLE event ADD CONSTRAINT ck_event_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED', 'ARCHIVED'));
ALTER TABLE event ADD CONSTRAINT ck_event_archived_at CHECK (status <> 'ARCHIVED' OR archived_at IS NOT NULL);

ALTER TABLE media_item ADD COLUMN thumbnail_image_id UUID;
ALTER TABLE media_item
    ADD CONSTRAINT fk_media_item_thumbnail_image_id FOREIGN KEY (thumbnail_image_id) REFERENCES image (id) ON DELETE SET NULL;
ALTER TABLE media_item DROP CONSTRAINT ck_media_item_category;
ALTER TABLE media_item ADD CONSTRAINT ck_media_item_category
    CHECK (category IN ('LECTURE', 'KHUTBAH', 'QURAN', 'COURSE', 'YOUTH', 'RECORDED_EVENT', 'OTHER'));

ALTER TABLE announcement ADD COLUMN image_id UUID;
ALTER TABLE announcement
    ADD CONSTRAINT fk_announcement_image_id FOREIGN KEY (image_id) REFERENCES image (id) ON DELETE SET NULL;

ALTER TABLE mosque ADD COLUMN prayer_times_url VARCHAR(500);

ALTER TABLE push_notification ALTER COLUMN content_id DROP NOT NULL;
ALTER TABLE push_notification ADD COLUMN audience VARCHAR(20);
ALTER TABLE push_notification DROP CONSTRAINT ck_push_notification_content_type;
ALTER TABLE push_notification ADD CONSTRAINT ck_push_notification_content_type
    CHECK (content_type IN ('ANNOUNCEMENT', 'EVENT', 'MEDIA', 'MESSAGE'));
ALTER TABLE push_notification ADD CONSTRAINT ck_push_notification_content
    CHECK ((content_type = 'MESSAGE') = (content_id IS NULL));
ALTER TABLE push_notification ADD CONSTRAINT ck_push_notification_audience
    CHECK (audience IS NULL OR audience IN ('ALL_FOLLOWERS', 'ANNOUNCEMENTS', 'EVENTS', 'MEDIA'));
