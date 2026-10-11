package nl.mijnummah.notification;

import nl.mijnummah.identity.AuthenticatedUser;

/** Sending push notifications, for content modules. */
public interface PushNotifications {

    /**
     * Sends the notification to the devices of the mosque's followers who did not switch the topic off. Returns at
     * once with status {@link PushStatus#SENDING}; delivery runs in the background.
     *
     * @throws nl.mijnummah.mosque.MosqueAccessDeniedException without {@code SEND_NOTIFICATIONS} for the mosque
     * @throws PushNotConfiguredException when no push service is configured
     * @throws PushAlreadySentException   when this content item was already sent
     */
    PushNotificationView send(AuthenticatedUser actor, PushRequest request);
}
