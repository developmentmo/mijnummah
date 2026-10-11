package nl.mijnummah.event.domain;

import java.time.Instant;
import java.util.Objects;

/** The editable fields of an event, validated together. Text is trimmed; blank optional fields become null. */
public record EventDetails(
        String title,
        String description,
        EventCategory category,
        Instant startsAt,
        Instant endsAt,
        String location,
        String imageUrl,
        Visibility visibility,
        boolean registrationEnabled,
        Integer capacity) {

    public EventDetails {
        title = Objects.requireNonNull(trimToNull(title), "title");
        description = trimToNull(description);
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(startsAt, "startsAt");
        location = trimToNull(location);
        imageUrl = trimToNull(imageUrl);
        visibility = visibility == null ? Visibility.PUBLIC : visibility;
        if (endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new InvalidEventException("endsAt must be after startsAt.");
        }
        if (capacity != null && capacity < 1) {
            throw new InvalidEventException("capacity must be at least 1.");
        }
        if (capacity != null && !registrationEnabled) {
            throw new InvalidEventException("capacity can only be set when registration is enabled.");
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
