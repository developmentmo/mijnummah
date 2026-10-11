package nl.mijnummah.media.application;

import java.time.LocalDate;
import java.util.UUID;

import nl.mijnummah.media.domain.MediaCategory;
import nl.mijnummah.media.domain.Visibility;

/** What an administrator submits; {@code videoUrl} is resolved to a provider reference by the service. */
public record MediaInput(String title, String description, String speaker, MediaCategory category, String videoUrl,
                         String thumbnailUrl, Integer durationSeconds, LocalDate recordedOn, UUID relatedEventId,
                         Visibility visibility) {
}
