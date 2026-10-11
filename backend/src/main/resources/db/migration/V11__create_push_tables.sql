-- Push notifications (fase 5).
--
-- push_device: the push token of an installed app, registered by a signed-in user. One token belongs to one user;
-- when another user signs in on the same device the token moves to that user. Tokens that the push service reports
-- as no longer valid are deleted.
--
-- push_notification: a push notification an administrator sent about one published content item. At most one per
-- content item, so followers are never notified twice about the same thing.

CREATE TABLE push_device (
    id           UUID         NOT NULL,
    user_id      UUID         NOT NULL,
    token        VARCHAR(512) NOT NULL,
    platform     VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    last_seen_at TIMESTAMPTZ  NOT NULL,
    version      BIGINT       NOT NULL,
    CONSTRAINT pk_push_device PRIMARY KEY (id),
    CONSTRAINT uk_push_device_token UNIQUE (token),
    CONSTRAINT fk_push_device_user_id FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT ck_push_device_platform CHECK (platform IN ('ANDROID', 'IOS'))
);

CREATE INDEX ix_push_device_user_id ON push_device (user_id);

CREATE TABLE push_notification (
    id              UUID         NOT NULL,
    mosque_id       UUID         NOT NULL,
    content_type    VARCHAR(20)  NOT NULL,
    content_id      UUID         NOT NULL,
    title           VARCHAR(100) NOT NULL,
    body            VARCHAR(250) NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    recipient_count INTEGER      NOT NULL,
    delivered_count INTEGER      NOT NULL,
    failed_count    INTEGER      NOT NULL,
    created_by      UUID         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    completed_at    TIMESTAMPTZ,
    version         BIGINT       NOT NULL,
    CONSTRAINT pk_push_notification PRIMARY KEY (id),
    CONSTRAINT uk_push_notification_content UNIQUE (content_type, content_id),
    CONSTRAINT fk_push_notification_mosque_id FOREIGN KEY (mosque_id) REFERENCES mosque (id),
    CONSTRAINT fk_push_notification_created_by FOREIGN KEY (created_by) REFERENCES app_user (id),
    CONSTRAINT ck_push_notification_content_type CHECK (content_type IN ('ANNOUNCEMENT', 'EVENT', 'MEDIA')),
    CONSTRAINT ck_push_notification_status CHECK (status IN ('SENDING', 'SENT', 'FAILED')),
    CONSTRAINT ck_push_notification_counts CHECK (recipient_count >= 0 AND delivered_count >= 0 AND failed_count >= 0
        AND delivered_count + failed_count <= recipient_count)
);

CREATE INDEX ix_push_notification_mosque_id ON push_notification (mosque_id, created_at DESC);
