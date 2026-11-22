package nl.mijnummah.notification;

import java.time.Instant;
import java.util.UUID;

/**
 * A sent (or still sending) push notification, for administrators. Who sent it stays internal (audit).
 *
 * @param contentId null for a free message
 * @param audience  who a free message was sent to; null for a notification about a content item
 */
public record PushNotificationView(
        UUID id,
        UUID mosqueId,
        PushContentType contentType,
        UUID contentId,
        PushTarget audience,
        String title,
        String body,
        PushStatus status,
        int recipientCount,
        int deliveredCount,
        int failedCount,
        Instant createdAt,
        Instant completedAt
) {
}
