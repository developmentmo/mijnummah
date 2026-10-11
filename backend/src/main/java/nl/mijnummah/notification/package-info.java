/**
 * Push notifications: the push tokens of signed-in app installations, and sending a notification about one
 * published content item to the followers of a mosque who did not switch that topic off.
 * <p>
 * Publishing content never sends a push notification (D-029). Sending is a separate action of a mosque ADMIN
 * ({@code SEND_NOTIFICATIONS}), at most once per content item. Content modules call {@link PushNotifications};
 * this module never reads content itself. The push service is chosen by {@code mijnummah.push.provider}
 * (Firebase Cloud Messaging, or none).
 */
@ApplicationModule(displayName = "Notification")
package nl.mijnummah.notification;

import org.springframework.modulith.ApplicationModule;
