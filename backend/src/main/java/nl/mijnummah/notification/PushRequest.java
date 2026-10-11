package nl.mijnummah.notification;

import java.util.Objects;
import java.util.UUID;

/**
 * A push notification about one published content item, as composed by the content module. The content module
 * has already checked that the item exists in this mosque and is published.
 */
public record PushRequest(UUID mosqueId, PushContentType contentType, UUID contentId, String title, String body) {

    public static final int MAX_TITLE_LENGTH = 100;
    public static final int MAX_BODY_LENGTH = 250;

    public PushRequest {
        Objects.requireNonNull(mosqueId, "mosqueId");
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(contentId, "contentId");
        title = shorten(Objects.requireNonNull(title, "title").strip(), MAX_TITLE_LENGTH);
        body = shorten(Objects.requireNonNull(body, "body").strip(), MAX_BODY_LENGTH);
    }

    /** Cuts at the limit with an ellipsis, so long content still fits a notification. */
    public static String shorten(String text, int max) {
        if (text.codePointCount(0, text.length()) <= max) {
            return text;
        }
        int end = text.offsetByCodePoints(0, max - 1);
        return text.substring(0, end).stripTrailing() + "…";
    }
}
