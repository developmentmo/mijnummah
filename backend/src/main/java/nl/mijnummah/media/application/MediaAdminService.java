package nl.mijnummah.media.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.common.audit.AuditEvent;
import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;
import nl.mijnummah.event.EventDirectory;
import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.media.domain.InvalidMediaException;
import nl.mijnummah.media.domain.MediaDetails;
import nl.mijnummah.media.domain.MediaItem;
import nl.mijnummah.media.domain.MediaStatus;
import nl.mijnummah.media.domain.VideoProviders;
import nl.mijnummah.media.domain.VideoReference;
import nl.mijnummah.media.infrastructure.MediaItemRepository;
import nl.mijnummah.media.infrastructure.MediaSpecifications;
import nl.mijnummah.mosque.MosqueAccessService;
import nl.mijnummah.mosque.MosquePermission;
import nl.mijnummah.notification.PushContentNotPublishedException;
import nl.mijnummah.notification.PushContentType;
import nl.mijnummah.notification.PushNotificationView;
import nl.mijnummah.notification.PushNotifications;
import nl.mijnummah.notification.PushRequest;
import nl.mijnummah.notification.PushText;
import nl.mijnummah.storage.ImageStorage;

/** Media management for a mosque's administrators and editors ({@link MosquePermission#MANAGE_CONTENT}). */
@Service
public class MediaAdminService {

    private static final Duration MAX_SCHEDULE_AHEAD = Duration.ofDays(366);

    private final MediaItemRepository items;
    private final MediaViews views;
    private final VideoProviders providers;
    private final EventDirectory events;
    private final MosqueAccessService access;
    private final PushNotifications push;
    private final ImageStorage images;
    private final ApplicationEventPublisher audit;
    private final Clock clock;

    public MediaAdminService(MediaItemRepository items, MediaViews views, VideoProviders providers,
                             EventDirectory events, MosqueAccessService access, PushNotifications push,
                             ImageStorage images, ApplicationEventPublisher audit, Clock clock) {
        this.items = items;
        this.views = views;
        this.providers = providers;
        this.events = events;
        this.access = access;
        this.push = push;
        this.images = images;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<MediaView> list(AuthenticatedUser actor, UUID mosqueId, MediaStatus status, String query,
                                Pageable pageable) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        Specification<MediaItem> spec = Specification.allOf(
                MediaSpecifications.ofMosque(mosqueId),
                MediaSpecifications.status(status),
                MediaSpecifications.matches(query));
        Page<MediaItem> page = items.findAll(spec, pageable);
        return new PageImpl<>(views.of(page.getContent()), page.getPageable(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public MediaView get(AuthenticatedUser actor, UUID mosqueId, UUID mediaId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        return views.of(load(mosqueId, mediaId));
    }

    @Transactional
    public MediaView create(AuthenticatedUser actor, UUID mosqueId, MediaInput input) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        MediaDetails details = toDetails(mosqueId, input, null);
        MediaItem item = saveUnique(MediaItem.draft(mosqueId, details, actor.id(), Instant.now(clock)));
        audit.publishEvent(AuditEvent.of(actor.id(), mosqueId, "VIDEO_CREATED", "VIDEO", item.getId()));
        return views.of(item);
    }

    @Transactional
    public MediaView update(AuthenticatedUser actor, UUID mosqueId, UUID mediaId, MediaInput input) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        MediaItem item = load(mosqueId, mediaId);
        item.update(toDetails(mosqueId, input, mediaId), actor.id(), Instant.now(clock));
        MediaItem saved = saveUnique(item);
        audit.publishEvent(AuditEvent.of(actor.id(), mosqueId, "VIDEO_UPDATED", "VIDEO", mediaId));
        return views.of(saved);
    }

    @Transactional
    public MediaView publish(AuthenticatedUser actor, UUID mosqueId, UUID mediaId, Instant publishAt) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        MediaItem item = load(mosqueId, mediaId);
        Instant now = Instant.now(clock);
        if (publishAt != null && publishAt.isAfter(now.plus(MAX_SCHEDULE_AHEAD))) {
            throw new InvalidMediaException("INVALID_PUBLICATION_DATE",
                    "The publication date can be at most a year ahead.");
        }
        item.publish(actor.id(), now, publishAt);
        audit.publishEvent(AuditEvent.of(actor.id(), mosqueId, "VIDEO_PUBLISHED", "VIDEO", mediaId));
        return views.of(item);
    }

    @Transactional
    public MediaView unpublish(AuthenticatedUser actor, UUID mosqueId, UUID mediaId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        MediaItem item = load(mosqueId, mediaId);
        item.unpublish(actor.id(), Instant.now(clock));
        audit.publishEvent(AuditEvent.of(actor.id(), mosqueId, "VIDEO_UNPUBLISHED", "VIDEO", mediaId));
        return views.of(item);
    }

    /**
     * Sends a push notification about a published item to the followers who want lectures and videos. By default
     * the title, and "Lezing van Imam X" (or the kind of video when there is no speaker).
     */
    @Transactional
    public PushNotificationView sendPush(AuthenticatedUser actor, UUID mosqueId, UUID mediaId, PushText text) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        MediaItem item = load(mosqueId, mediaId);
        if (!item.isLive(Instant.now(clock))) {
            throw new PushContentNotPublishedException(
                    "Only a published video whose publication date has come can be sent as a push notification.");
        }
        MediaDetails details = item.details();
        return push.send(actor, new PushRequest(mosqueId, PushContentType.MEDIA, mediaId,
                text.titleOr(details.title()), text.bodyOr(defaultPushBody(details))));
    }

    static String defaultPushBody(MediaDetails details) {
        String kind = switch (details.category()) {
            case LECTURE -> "Lezing";
            case KHUTBAH -> "Vrijdagpreek";
            case QURAN -> "Koranrecitatie";
            case COURSE -> "Les";
            case YOUTH -> "Video voor jongeren";
            case RECORDED_EVENT -> "Opname van een activiteit";
            case OTHER -> "Video";
        };
        return details.speaker() == null ? kind + " nu te bekijken in de app" : kind + " van " + details.speaker();
    }

    /** Deletes only the reference; the video at the provider is not touched. */
    @Transactional
    public void delete(AuthenticatedUser actor, UUID mosqueId, UUID mediaId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        items.delete(load(mosqueId, mediaId));
        audit.publishEvent(AuditEvent.of(actor.id(), mosqueId, "VIDEO_DELETED", "VIDEO", mediaId));
    }

    private MediaDetails toDetails(UUID mosqueId, MediaInput input, UUID currentId) {
        VideoReference video = providers.resolve(input.videoUrl());
        items.findByMosqueIdAndVideoProviderAndVideoId(mosqueId, video.provider(), video.videoId())
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new DuplicateVideoException();
                });
        if (input.relatedEventId() != null && !events.existsInMosque(input.relatedEventId(), mosqueId)) {
            throw new InvalidMediaException("INVALID_RELATED_EVENT", "relatedEventId is not an event of this mosque.");
        }
        if (input.thumbnailImageId() != null) {
            images.requireOwnedBy(input.thumbnailImageId(), mosqueId);
        }
        return new MediaDetails(input.title(), input.description(), input.speaker(), input.category(), video,
                input.thumbnailUrl(), input.thumbnailImageId(), input.durationSeconds(), input.recordedOn(), input.relatedEventId(),
                input.visibility());
    }

    private MediaItem saveUnique(MediaItem item) {
        try {
            return items.saveAndFlush(item);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateVideoException();
        }
    }

    private MediaItem load(UUID mosqueId, UUID mediaId) {
        return items.findByIdAndMosqueId(mediaId, mosqueId).orElseThrow(MediaNotFound::exception);
    }

    static class DuplicateVideoException extends ApplicationException {

        DuplicateVideoException() {
            super(ErrorCategory.CONFLICT, "VIDEO_ALREADY_ADDED", "This video is already in the media library.");
        }
    }
}
