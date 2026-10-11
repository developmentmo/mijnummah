package nl.mijnummah.media.application;

import java.time.Instant;
import java.util.UUID;

import nl.mijnummah.event.EventSummary;
import nl.mijnummah.media.domain.MediaDetails;
import nl.mijnummah.media.domain.MediaStatus;

/**
 * A media item with the provider URLs resolved.
 *
 * @param thumbnailUrl the administrator's thumbnail, or the provider's default
 * @param relatedEvent the linked event when it is publicly listed (public views) or exists (admin views)
 */
public record MediaView(UUID id, UUID mosqueId, MediaDetails details, String watchUrl, String embedUrl,
                        String thumbnailUrl, EventSummary relatedEvent, MediaStatus status, Instant publishedAt,
                        Instant createdAt, Instant updatedAt) {
}
