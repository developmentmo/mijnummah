package nl.mijnummah.media.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
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
    private final Clock clock;

    public PublicMediaService(MediaItemRepository items, MediaViews views, MosqueDirectory mosques, Clock clock) {
        this.items = items;
        this.views = views;
        this.mosques = mosques;
        this.clock = clock;
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

    /**
     * The library across mosques for the central app: all active mosques, or only those among {@code mosqueSlugs}
     * when given (unknown and inactive slugs are ignored).
     */
    @Transactional(readOnly = true)
    public Page<MediaView> listAcross(Collection<String> mosqueSlugs, AuthenticatedUser viewer, String query,
                                      MediaCategory category, Pageable pageable) {
        Set<UUID> mosqueIds = mosqueSlugs == null || mosqueSlugs.isEmpty()
                ? mosques.activeIds()
                : mosques.findActiveBySlugs(mosqueSlugs).keySet();
        if (mosqueIds.isEmpty()) {
            return Page.empty(pageable);
        }
        Specification<MediaItem> spec = Specification.allOf(
                MediaSpecifications.ofMosques(mosqueIds),
                MediaSpecifications.liveAt(Instant.now(clock)),
                MediaSpecifications.visibilityIn(visibleTo(viewer)),
                MediaSpecifications.category(category),
                MediaSpecifications.matches(query));
        Page<MediaItem> page = items.findAll(spec, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                NEWEST_FIRST));
        return new PageImpl<>(views.of(page.getContent()), page.getPageable(), page.getTotalElements());
    }

    /** One public item of any active mosque, e.g. opened from a push notification or the favourites. */
    @Transactional(readOnly = true)
    public MediaView get(UUID mediaId, AuthenticatedUser viewer) {
        return views.of(loadAny(mediaId, viewer));
    }

    @Transactional(readOnly = true)
    public List<MediaView> related(UUID mediaId, AuthenticatedUser viewer) {
        return related(loadAny(mediaId, viewer), viewer);
    }

    /** The item when it is live, visible to the viewer and its mosque is active. */
    MediaItem loadAny(UUID mediaId, AuthenticatedUser viewer) {
        return items.findById(mediaId)
                .filter(item -> item.isLive(Instant.now(clock)))
                .filter(item -> visibleTo(viewer).contains(item.getVisibility()))
                .filter(item -> !mosques.findActiveByIds(List.of(item.getMosqueId())).isEmpty())
                .orElseThrow(MediaNotFound::exception);
    }

    @Transactional(readOnly = true)
    public MediaView get(String mosqueSlug, UUID mediaId, AuthenticatedUser viewer) {
        return views.of(load(mosqueSlug, mediaId, viewer));
    }

    /** Up to {@value #RELATED_LIMIT} other items with the same category or speaker, newest first. */
    @Transactional(readOnly = true)
    public List<MediaView> related(String mosqueSlug, UUID mediaId, AuthenticatedUser viewer) {
        return related(load(mosqueSlug, mediaId, viewer), viewer);
    }

    private List<MediaView> related(MediaItem item, AuthenticatedUser viewer) {
        Specification<MediaItem> spec = Specification.allOf(
                visibleIn(item.getMosqueId(), viewer),
                MediaSpecifications.notId(item.getId()),
                MediaSpecifications.relatedTo(item.getCategory(), item.getSpeaker()));
        return views.of(items.findAll(spec, PageRequest.of(0, RELATED_LIMIT, NEWEST_FIRST)).getContent());
    }

    private MediaItem load(String mosqueSlug, UUID mediaId, AuthenticatedUser viewer) {
        MosqueRef mosque = mosques.requireActiveBySlug(mosqueSlug);
        return items.findByIdAndMosqueId(mediaId, mosque.id())
                .filter(item -> item.isLive(Instant.now(clock)))
                .filter(item -> visibleTo(viewer).contains(item.getVisibility()))
                .orElseThrow(MediaNotFound::exception);
    }

    private Specification<MediaItem> visibleIn(UUID mosqueId, AuthenticatedUser viewer) {
        return Specification.allOf(
                MediaSpecifications.ofMosque(mosqueId),
                MediaSpecifications.liveAt(Instant.now(clock)),
                MediaSpecifications.visibilityIn(visibleTo(viewer)));
    }

    static Set<Visibility> visibleTo(AuthenticatedUser viewer) {
        return viewer == null ? Set.of(Visibility.PUBLIC) : Set.of(Visibility.PUBLIC, Visibility.REGISTERED_USERS);
    }
}
