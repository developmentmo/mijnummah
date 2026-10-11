package nl.mijnummah.notification;

import java.time.Instant;
import java.util.UUID;

/** A sent (or still sending) push notification, for administrators. Who sent it stays internal (audit). */
public record PushNotificationView(
        UUID id,
        UUID mosqueId,
        PushContentType contentType,
        UUID contentId,
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
