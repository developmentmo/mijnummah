package nl.mijnummah.follow.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import nl.mijnummah.follow.NotificationTopic;

/**
 * A followed mosque with the follower's notification preferences (every topic present).
 *
 * @param mosqueLogoUrl path of the mosque's logo relative to the API base URL; null when there is none
 * @param favorite      shown first in the app
 */
public record FollowView(UUID mosqueId, String mosqueSlug, String mosqueName, String mosqueLogoUrl,
                         Instant followedSince, boolean favorite, Map<NotificationTopic, Boolean> notifications) {
}
