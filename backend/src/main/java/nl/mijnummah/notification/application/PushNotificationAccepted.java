package nl.mijnummah.notification.application;

import java.util.List;
import java.util.UUID;

/** Published in the transaction that stored the notification; delivery starts after commit. */
record PushNotificationAccepted(UUID notificationId, PushMessage message, List<String> tokens) {
}
