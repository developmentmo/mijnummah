package nl.mijnummah.media.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

/** Media management for a mosque's administrators and editors ({@link MosquePermission#MANAGE_CONTENT}). */
@Service
public class MediaAdminService {

    private final MediaItemRepository items;
    private final MediaViews views;
    private final VideoProviders providers;
    private final EventDirectory events;
    private final MosqueAccessService access;
    private final Clock clock;

    public MediaAdminService(MediaItemRepository items, MediaViews views, VideoProviders providers,
                             EventDirectory events, MosqueAccessService access, Clock clock) {
        this.items = items;
        this.views = views;
        this.providers = providers;
        this.events = events;
        this.access = access;
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
        MediaItem item = MediaItem.draft(mosqueId, details, actor.id(), Instant.now(clock));
        return views.of(saveUnique(item));
    }

    @Transactional
    public MediaView update(AuthenticatedUser actor, UUID mosqueId, UUID mediaId, MediaInput input) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        MediaItem item = load(mosqueId, mediaId);
        item.update(toDetails(mosqueId, input, mediaId), actor.id(), Instant.now(clock));
        return views.of(saveUnique(item));
    }

    @Transactional
    public MediaView publish(AuthenticatedUser actor, UUID mosqueId, UUID mediaId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        MediaItem item = load(mosqueId, mediaId);
        item.publish(actor.id(), Instant.now(clock));
        return views.of(item);
    }

    @Transactional
    public MediaView unpublish(AuthenticatedUser actor, UUID mosqueId, UUID mediaId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        MediaItem item = load(mosqueId, mediaId);
        item.unpublish(actor.id(), Instant.now(clock));
        return views.of(item);
    }

    /** Deletes only the reference; the video at the provider is not touched. */
    @Transactional
    public void delete(AuthenticatedUser actor, UUID mosqueId, UUID mediaId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        items.delete(load(mosqueId, mediaId));
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
        return new MediaDetails(input.title(), input.description(), input.speaker(), input.category(), video,
                input.thumbnailUrl(), input.durationSeconds(), input.recordedOn(), input.relatedEventId(),
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
