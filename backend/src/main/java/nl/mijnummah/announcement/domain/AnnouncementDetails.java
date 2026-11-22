package nl.mijnummah.announcement.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** The editable fields of an announcement; {@code imageId} is an uploaded image of the same mosque. */
public record AnnouncementDetails(String title, String body, boolean featured, Instant expiresAt, UUID imageId) {

    public AnnouncementDetails {
        title = Objects.requireNonNull(trimToNull(title), "title");
        body = Objects.requireNonNull(trimToNull(body), "body");
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
