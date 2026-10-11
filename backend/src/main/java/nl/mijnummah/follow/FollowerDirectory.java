package nl.mijnummah.follow;

import java.util.List;
import java.util.UUID;

/** Read-only lookup of followers for other modules (the notification module decides who receives a push). */
public interface FollowerDirectory {

    /**
     * Users who follow the mosque and did not switch off the topic (a topic is on by default). Whether the accounts
     * are active is not checked here.
     */
    List<UUID> followersToNotify(UUID mosqueId, NotificationTopic topic);
}
