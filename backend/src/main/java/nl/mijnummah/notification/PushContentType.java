package nl.mijnummah.notification;

import nl.mijnummah.follow.NotificationTopic;

/**
 * What a push notification is about; decides which notification preference applies. MESSAGE is a free message from
 * the mosque, without a content item; its audience is chosen with a {@link PushTarget}.
 */
public enum PushContentType {

    ANNOUNCEMENT(NotificationTopic.ANNOUNCEMENTS),
    EVENT(NotificationTopic.EVENTS),
    MEDIA(NotificationTopic.MEDIA),
    MESSAGE(null);

    private final NotificationTopic topic;

    PushContentType(NotificationTopic topic) {
        this.topic = topic;
    }

    /** @throws IllegalStateException for MESSAGE, whose audience is a {@link PushTarget} */
    public NotificationTopic topic() {
        if (topic == null) {
            throw new IllegalStateException("A message has no topic; use its PushTarget.");
        }
        return topic;
    }
}
