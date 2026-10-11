package nl.mijnummah.notification.application;

import java.util.Map;

/** What a device shows, plus data the app uses to open the right screen when the notification is tapped. */
public record PushMessage(String title, String body, Map<String, String> data) {

    public PushMessage {
        data = Map.copyOf(data);
    }
}
