package nl.mijnummah.media.application;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import nl.mijnummah.event.EventDirectory;
import nl.mijnummah.event.EventSummary;
import nl.mijnummah.media.domain.MediaItem;
import nl.mijnummah.media.domain.VideoProvider;
import nl.mijnummah.media.domain.VideoProviders;

@Component
class MediaViews {

    private final VideoProviders providers;
    private final EventDirectory events;

    MediaViews(VideoProviders providers, EventDirectory events) {
        this.providers = providers;
        this.events = events;
    }

    List<MediaView> of(Collection<MediaItem> items) {
        List<UUID> eventIds = items.stream().map(MediaItem::getRelatedEventId).filter(Objects::nonNull).distinct()
                .toList();
        Map<UUID, EventSummary> related = eventIds.isEmpty() ? Map.of() : events.findPublicSummaries(eventIds);
        return items.stream()
                .map(item -> view(item, item.getRelatedEventId() == null ? null : related.get(item.getRelatedEventId())))
                .toList();
    }

    MediaView of(MediaItem item) {
        return of(List.of(item)).getFirst();
    }

    private MediaView view(MediaItem item, EventSummary relatedEvent) {
        var details = item.details();
        VideoProvider provider = providers.get(details.video().provider());
        String videoId = details.video().videoId();
        String thumbnail = details.thumbnailUrl() != null ? details.thumbnailUrl() : provider.defaultThumbnailUrl(videoId);
        return new MediaView(item.getId(), item.getMosqueId(), details, provider.watchUrl(videoId),
                provider.embedUrl(videoId), thumbnail, relatedEvent, item.getStatus(), item.getPublishedAt(),
                item.getCreatedAt(), item.getUpdatedAt());
    }
}
