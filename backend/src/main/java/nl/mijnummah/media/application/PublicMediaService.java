package nl.mijnummah.media.application;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.media.domain.MediaCategory;
import nl.mijnummah.media.domain.MediaItem;
import nl.mijnummah.media.domain.MediaStatus;
import nl.mijnummah.media.domain.Visibility;
import nl.mijnummah.media.infrastructure.MediaItemRepository;
import nl.mijnummah.media.infrastructure.MediaSpecifications;
import nl.mijnummah.mosque.MosqueDirectory;
import nl.mijnummah.mosque.MosqueRef;

/** The public media library of an active mosque: published items, filtered by visibility. */
@Service
public class PublicMediaService {

    public static final int RELATED_LIMIT = 4;
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.asc("id"));

    private final MediaItemRepository items;
    private final MediaViews views;
    private final MosqueDirectory mosques;

    public PublicMediaService(MediaItemRepository items, MediaViews views, MosqueDirectory mosques) {
        this.items = items;
        this.views = views;
        this.mosques = mosques;
    }

    @Transactional(readOnly = true)
    public Page<MediaView> list(String mosqueSlug, AuthenticatedUser viewer, String query, MediaCategory category,
                                Pageable pageable) {
        MosqueRef mosque = mosques.requireActiveBySlug(mosqueSlug);
        Specification<MediaItem> spec = Specification.allOf(
                visibleIn(mosque.id(), viewer),
                MediaSpecifications.category(category),
                MediaSpecifications.matches(query));
        Page<MediaItem> page = items.findAll(spec, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                NEWEST_FIRST));
        return new PageImpl<>(views.of(page.getContent()), page.getPageable(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public MediaView get(String mosqueSlug, UUID mediaId, AuthenticatedUser viewer) {
        return views.of(load(mosqueSlug, mediaId, viewer));
    }

    /** Up to {@value #RELATED_LIMIT} other items with the same category or speaker, newest first. */
    @Transactional(readOnly = true)
    public List<MediaView> related(String mosqueSlug, UUID mediaId, AuthenticatedUser viewer) {
        MediaItem item = load(mosqueSlug, mediaId, viewer);
        Specification<MediaItem> spec = Specification.allOf(
                visibleIn(item.getMosqueId(), viewer),
                MediaSpecifications.notId(item.getId()),
                MediaSpecifications.relatedTo(item.getCategory(), item.getSpeaker()));
        return views.of(items.findAll(spec, PageRequest.of(0, RELATED_LIMIT, NEWEST_FIRST)).getContent());
    }

    private MediaItem load(String mosqueSlug, UUID mediaId, AuthenticatedUser viewer) {
        MosqueRef mosque = mosques.requireActiveBySlug(mosqueSlug);
        return items.findByIdAndMosqueId(mediaId, mosque.id())
                .filter(item -> item.getStatus() == MediaStatus.PUBLISHED)
                .filter(item -> visibleTo(viewer).contains(item.getVisibility()))
                .orElseThrow(MediaNotFound::exception);
    }

    private static Specification<MediaItem> visibleIn(UUID mosqueId, AuthenticatedUser viewer) {
        return Specification.allOf(
                MediaSpecifications.ofMosque(mosqueId),
                MediaSpecifications.status(MediaStatus.PUBLISHED),
                MediaSpecifications.visibilityIn(visibleTo(viewer)));
    }

    private static Set<Visibility> visibleTo(AuthenticatedUser viewer) {
        return viewer == null ? Set.of(Visibility.PUBLIC) : Set.of(Visibility.PUBLIC, Visibility.REGISTERED_USERS);
    }
}
