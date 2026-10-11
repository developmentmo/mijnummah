-- Lectures and other recordings. The video itself is NEVER stored here (nor anywhere in this system): only a
-- reference to an external provider (video_provider + video_id, e.g. YOUTUBE + the 11-character video ID).

CREATE TABLE media_item (
    id               UUID          NOT NULL,
    mosque_id        UUID          NOT NULL,
    title            VARCHAR(150)  NOT NULL,
    description      VARCHAR(10000),
    speaker          VARCHAR(100),
    category         VARCHAR(30)   NOT NULL,
    video_provider   VARCHAR(20)   NOT NULL,
    video_id         VARCHAR(100)  NOT NULL,
    thumbnail_url    VARCHAR(500),
    duration_seconds INTEGER,
    recorded_on      DATE,
    related_event_id UUID,
    visibility       VARCHAR(20)   NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    published_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ   NOT NULL,
    created_by       UUID          NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL,
    updated_by       UUID          NOT NULL,
    version          BIGINT        NOT NULL,
    CONSTRAINT pk_media_item PRIMARY KEY (id),
    CONSTRAINT fk_media_item_mosque_id FOREIGN KEY (mosque_id) REFERENCES mosque (id),
    -- the related event must belong to the same mosque; deleting a draft event unlinks it
    CONSTRAINT fk_media_item_related_event FOREIGN KEY (related_event_id, mosque_id)
        REFERENCES event (id, mosque_id) ON DELETE SET NULL (related_event_id),
    CONSTRAINT fk_media_item_created_by FOREIGN KEY (created_by) REFERENCES app_user (id),
    CONSTRAINT fk_media_item_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id),
    CONSTRAINT uk_media_item_mosque_video UNIQUE (mosque_id, video_provider, video_id),
    CONSTRAINT ck_media_item_category CHECK (category IN ('LECTURE', 'KHUTBAH', 'QURAN', 'COURSE', 'YOUTH', 'OTHER')),
    CONSTRAINT ck_media_item_video_provider CHECK (video_provider IN ('YOUTUBE')),
    CONSTRAINT ck_media_item_visibility CHECK (visibility IN ('PUBLIC', 'REGISTERED_USERS')),
    CONSTRAINT ck_media_item_status CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT ck_media_item_duration CHECK (duration_seconds IS NULL OR duration_seconds > 0),
    CONSTRAINT ck_media_item_published_at CHECK (status = 'DRAFT' OR published_at IS NOT NULL)
);

CREATE INDEX ix_media_item_mosque_status_published_at ON media_item (mosque_id, status, published_at DESC);
CREATE INDEX ix_media_item_related_event_id ON media_item (related_event_id);
