package nl.mijnummah.announcement.domain;

import java.time.Instant;
import java.util.Objects;

/** The editable fields of an announcement. */
public record AnnouncementDetails(String title, String body, boolean featured, Instant expiresAt) {

    public AnnouncementDetails {
        title = Objects.requireNonNull(trimToNull(title), "title");
        body = Objects.requireNonNull(trimToNull(body), "body");
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
