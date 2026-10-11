package nl.mijnummah.notification;

import jakarta.validation.constraints.Size;

/**
 * Optional wording of a push notification, chosen by the administrator. A missing or blank field falls back to the
 * text the content module composes from the item.
 */
public record PushText(@Size(max = PushRequest.MAX_TITLE_LENGTH) String title,
                       @Size(max = PushRequest.MAX_BODY_LENGTH) String body) {

    public static final PushText DEFAULT = new PushText(null, null);

    public String titleOr(String fallback) {
        return title == null || title.isBlank() ? fallback : title;
    }

    public String bodyOr(String fallback) {
        return body == null || body.isBlank() ? fallback : body;
    }
}
