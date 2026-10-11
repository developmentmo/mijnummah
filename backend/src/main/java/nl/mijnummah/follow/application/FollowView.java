package nl.mijnummah.follow.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import nl.mijnummah.follow.NotificationTopic;

/** A followed mosque with the follower's notification preferences (every topic present). */
public record FollowView(UUID mosqueId, String mosqueSlug, String mosqueName, Instant followedSince,
                         Map<NotificationTopic, Boolean> notifications) {
}
