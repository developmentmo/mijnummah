package nl.mijnummah.event.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.event.domain.Event;
import nl.mijnummah.event.domain.EventCategory;
import nl.mijnummah.event.domain.EventStatus;
import nl.mijnummah.event.domain.Visibility;
import nl.mijnummah.event.infrastructure.EventRepository;
import nl.mijnummah.event.infrastructure.EventSpecifications;
import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.mosque.MosqueDirectory;
import nl.mijnummah.mosque.MosqueRef;

/**
 * The public agenda of an active mosque: published and cancelled events, never drafts. Anonymous callers see only
 * events with visibility PUBLIC; signed-in users also see REGISTERED_USERS.
 */
@Service
public class PublicEventService {

    private static final Set<EventStatus> PUBLIC_STATUSES = Set.of(EventStatus.PUBLISHED, EventStatus.CANCELLED);

    private final EventRepository events;
    private final EventViews views;
    private final MosqueDirectory mosques;
    private final Clock clock;

    public PublicEventService(EventRepository events, EventViews views, MosqueDirectory mosques, Clock clock) {
        this.events = events;
        this.views = views;
        this.mosques = mosques;
        this.clock = clock;
    }

    /**
     * @param from only events that are not over yet at this moment; defaults to now (the upcoming agenda)
     * @param to   only events starting before this moment; optional
     */
    @Transactional(readOnly = true)
    public Page<EventView> list(String mosqueSlug, AuthenticatedUser viewer, EventCategory category, Instant from,
                                Instant to, Pageable pageable) {
        MosqueRef mosque = mosques.requireActiveBySlug(mosqueSlug);
        Specification<Event> spec = Specification.allOf(
                EventSpecifications.ofMosque(mosque.id()),
                EventSpecifications.statusIn(PUBLIC_STATUSES),
                EventSpecifications.visibilityIn(visibleTo(viewer)),
                EventSpecifications.category(category),
                EventSpecifications.notOverBefore(from == null ? Instant.now(clock) : from),
                EventSpecifications.startsBefore(to));
        Page<Event> page = events.findAll(spec, pageable);
        List<EventView> content = views.of(page.getContent(), viewerId(viewer));
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    /**
     * The agenda across mosques for the central app: all active mosques, or only those among {@code mosqueSlugs}
     * when given (unknown and inactive slugs are ignored, so a list of only those is empty).
     */
    @Transactional(readOnly = true)
    public Page<EventView> listAcross(Collection<String> mosqueSlugs, AuthenticatedUser viewer, EventCategory category,
                                      Instant from, Instant to, Pageable pageable) {
        Set<UUID> mosqueIds = mosqueSlugs == null || mosqueSlugs.isEmpty()
                ? mosques.activeIds()
                : mosques.findActiveBySlugs(mosqueSlugs).keySet();
        if (mosqueIds.isEmpty()) {
            return Page.empty(pageable);
        }
        Specification<Event> spec = Specification.allOf(
                EventSpecifications.ofMosques(mosqueIds),
                EventSpecifications.statusIn(PUBLIC_STATUSES),
                EventSpecifications.visibilityIn(visibleTo(viewer)),
                EventSpecifications.category(category),
                EventSpecifications.notOverBefore(from == null ? Instant.now(clock) : from),
                EventSpecifications.startsBefore(to));
        Page<Event> page = events.findAll(spec, pageable);
        List<EventView> content = views.of(page.getContent(), viewerId(viewer));
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    /** One public event of any active mosque, e.g. opened from a push notification. */
    @Transactional(readOnly = true)
    public EventView get(UUID eventId, AuthenticatedUser viewer) {
        Event event = events.findById(eventId)
                .filter(candidate -> isVisible(candidate, viewer))
                .filter(candidate -> !mosques.findActiveByIds(List.of(candidate.getMosqueId())).isEmpty())
                .orElseThrow(EventNotFound::exception);
        return views.of(event, viewerId(viewer));
    }

    @Transactional(readOnly = true)
    public EventView get(String mosqueSlug, UUID eventId, AuthenticatedUser viewer) {
        MosqueRef mosque = mosques.requireActiveBySlug(mosqueSlug);
        Event event = events.findByIdAndMosqueId(eventId, mosque.id())
                .filter(candidate -> isVisible(candidate, viewer))
                .orElseThrow(EventNotFound::exception);
        return views.of(event, viewerId(viewer));
    }

    static boolean isVisible(Event event, AuthenticatedUser viewer) {
        return event.isPubliclyListed() && visibleTo(viewer).contains(event.getVisibility());
    }

    static Set<Visibility> visibleTo(AuthenticatedUser viewer) {
        return viewer == null ? Set.of(Visibility.PUBLIC) : Set.of(Visibility.PUBLIC, Visibility.REGISTERED_USERS);
    }

    private static UUID viewerId(AuthenticatedUser viewer) {
        return viewer == null ? null : viewer.id();
    }
}
