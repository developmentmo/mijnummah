package nl.mijnummah.notification;

import nl.mijnummah.follow.NotificationTopic;

/** What a push notification is about; decides which notification preference applies. */
public enum PushContentType {

    ANNOUNCEMENT(NotificationTopic.ANNOUNCEMENTS),
    EVENT(NotificationTopic.EVENTS),
    MEDIA(NotificationTopic.MEDIA);

    private final NotificationTopic topic;

    PushContentType(NotificationTopic topic) {
        this.topic = topic;
    }

    public NotificationTopic topic() {
        return topic;
    }
}
