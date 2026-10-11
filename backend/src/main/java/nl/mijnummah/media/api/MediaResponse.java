package nl.mijnummah.media.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import nl.mijnummah.event.EventSummary;
import nl.mijnummah.media.application.MediaView;
import nl.mijnummah.media.domain.MediaCategory;
import nl.mijnummah.media.domain.MediaStatus;
import nl.mijnummah.media.domain.VideoProviderType;
import nl.mijnummah.media.domain.Visibility;

/** A media item. The video is played from the provider ({@code video.embedUrl} or {@code video.watchUrl}). */
record MediaResponse(UUID id, String title, String description, String speaker, MediaCategory category, Video video,
                     String thumbnailUrl, Integer durationSeconds, LocalDate recordedOn, EventSummary relatedEvent,
                     Visibility visibility, Instant publishedAt) {

    record Video(VideoProviderType provider, String videoId, String watchUrl, String embedUrl) {
    }

    static MediaResponse from(MediaView view) {
        var d = view.details();
        return new MediaResponse(view.id(), d.title(), d.description(), d.speaker(), d.category(), videoOf(view),
                view.thumbnailUrl(), d.durationSeconds(), d.recordedOn(), view.relatedEvent(), d.visibility(),
                view.publishedAt());
    }

    static Video videoOf(MediaView view) {
        return new Video(view.details().video().provider(), view.details().video().videoId(), view.watchUrl(),
                view.embedUrl());
    }

    /** Admin view: adds status, the raw thumbnail setting and audit timestamps. */
    record Admin(UUID id, UUID mosqueId, String title, String description, String speaker, MediaCategory category,
                 Video video, String thumbnailUrl, String customThumbnailUrl, Integer durationSeconds,
                 LocalDate recordedOn, UUID relatedEventId, Visibility visibility, MediaStatus status,
                 Instant publishedAt, Instant createdAt, Instant updatedAt) {

        static Admin from(MediaView view) {
            var d = view.details();
            return new Admin(view.id(), view.mosqueId(), d.title(), d.description(), d.speaker(), d.category(),
                    videoOf(view), view.thumbnailUrl(), d.thumbnailUrl(), d.durationSeconds(), d.recordedOn(),
                    d.relatedEventId(), d.visibility(), view.status(), view.publishedAt(), view.createdAt(),
                    view.updatedAt());
        }
    }
}
