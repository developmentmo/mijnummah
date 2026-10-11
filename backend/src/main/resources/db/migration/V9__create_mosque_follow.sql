-- Following a mosque (not membership, see D-011): the user wants to see and, later, be notified about its
-- content. Notification preferences per topic belong to a follow and disappear with it.

CREATE TABLE mosque_follow (
    user_id    UUID        NOT NULL,
    mosque_id  UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_mosque_follow PRIMARY KEY (user_id, mosque_id),
    CONSTRAINT fk_mosque_follow_user_id FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_mosque_follow_mosque_id FOREIGN KEY (mosque_id) REFERENCES mosque (id)
);

CREATE INDEX ix_mosque_follow_mosque_id ON mosque_follow (mosque_id);

-- One row per topic the user changed; a missing row means the default (enabled).
CREATE TABLE notification_preference (
    user_id    UUID        NOT NULL,
    mosque_id  UUID        NOT NULL,
    topic      VARCHAR(30) NOT NULL,
    enabled    BOOLEAN     NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_notification_preference PRIMARY KEY (user_id, mosque_id, topic),
    CONSTRAINT fk_notification_preference_follow FOREIGN KEY (user_id, mosque_id)
        REFERENCES mosque_follow (user_id, mosque_id) ON DELETE CASCADE,
    CONSTRAINT ck_notification_preference_topic CHECK (topic IN ('ANNOUNCEMENTS', 'EVENTS', 'MEDIA'))
);
