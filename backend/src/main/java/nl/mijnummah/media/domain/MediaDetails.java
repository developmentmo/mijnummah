package nl.mijnummah.media.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** The editable fields of a media item. Text is trimmed; blank optional fields become null. */
public record MediaDetails(
        String title,
        String description,
        String speaker,
        MediaCategory category,
        VideoReference video,
        String thumbnailUrl,
        Integer durationSeconds,
        LocalDate recordedOn,
        UUID relatedEventId,
        Visibility visibility) {

    public MediaDetails {
        title = Objects.requireNonNull(trimToNull(title), "title");
        description = trimToNull(description);
        speaker = trimToNull(speaker);
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(video, "video");
        thumbnailUrl = trimToNull(thumbnailUrl);
        if (durationSeconds != null && durationSeconds < 1) {
            throw new InvalidMediaException("INVALID_MEDIA", "durationSeconds must be at least 1.");
        }
        visibility = visibility == null ? Visibility.PUBLIC : visibility;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
