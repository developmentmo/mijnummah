package nl.mijnummah.notification;

import java.util.EnumSet;
import java.util.Set;

import nl.mijnummah.follow.NotificationTopic;

/**
 * Who a free push message goes to: followers of the mosque who receive notifications about a topic, or about at
 * least one topic ({@code ALL_FOLLOWERS}). Followers who switched every topic off are never addressed.
 */
public enum PushTarget {

    ALL_FOLLOWERS(EnumSet.allOf(NotificationTopic.class)),
    ANNOUNCEMENTS(EnumSet.of(NotificationTopic.ANNOUNCEMENTS)),
    EVENTS(EnumSet.of(NotificationTopic.EVENTS)),
    MEDIA(EnumSet.of(NotificationTopic.MEDIA));

    private final Set<NotificationTopic> topics;

    PushTarget(Set<NotificationTopic> topics) {
        this.topics = topics;
    }

    public Set<NotificationTopic> topics() {
        return topics;
    }
}
