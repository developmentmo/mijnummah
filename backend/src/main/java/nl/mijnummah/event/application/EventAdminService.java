package nl.mijnummah.event.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;
import nl.mijnummah.event.domain.Event;
import nl.mijnummah.event.domain.EventDetails;
import nl.mijnummah.event.domain.EventRuleViolationException;
import nl.mijnummah.event.domain.EventStatus;
import nl.mijnummah.event.domain.RegistrationStatus;
import nl.mijnummah.event.infrastructure.EventRegistrationRepository;
import nl.mijnummah.event.infrastructure.EventRepository;
import nl.mijnummah.event.infrastructure.EventSpecifications;
import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.mosque.MosqueAccessService;
import nl.mijnummah.mosque.MosqueDirectory;
import nl.mijnummah.mosque.MosquePermission;
import nl.mijnummah.notification.PushContentNotPublishedException;
import nl.mijnummah.notification.PushContentType;
import nl.mijnummah.notification.PushNotificationView;
import nl.mijnummah.notification.PushNotifications;
import nl.mijnummah.notification.PushRequest;
import nl.mijnummah.notification.PushText;

/**
 * Event management for administrators and editors of a mosque. Every method first checks
 * {@link MosquePermission#MANAGE_CONTENT} for the mosque from the route, then loads the event by (id, mosque).
 */
@Service
public class EventAdminService {

    private static final DateTimeFormatter PUSH_DATE =
            DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.forLanguageTag("nl"));

    private final EventRepository events;
    private final EventRegistrationRepository registrations;
    private final EventViews views;
    private final MosqueAccessService access;
    private final MosqueDirectory mosques;
    private final PushNotifications push;
    private final Clock clock;

    public EventAdminService(EventRepository events, EventRegistrationRepository registrations, EventViews views,
                             MosqueAccessService access, MosqueDirectory mosques, PushNotifications push,
                             Clock clock) {
        this.events = events;
        this.registrations = registrations;
        this.views = views;
        this.access = access;
        this.mosques = mosques;
        this.push = push;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<EventView> list(AuthenticatedUser actor, UUID mosqueId, EventStatus status, Instant from,
                                Pageable pageable) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        Specification<Event> spec = Specification.allOf(
                EventSpecifications.ofMosque(mosqueId),
                status == null ? Specification.unrestricted() : EventSpecifications.statusIn(List.of(status)),
                EventSpecifications.notOverBefore(from));
        Page<Event> page = events.findAll(spec, pageable);
        return new PageImpl<>(views.of(page.getContent(), null), page.getPageable(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public EventView get(AuthenticatedUser actor, UUID mosqueId, UUID eventId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        return views.of(load(mosqueId, eventId), null);
    }

    @Transactional
    public EventView create(AuthenticatedUser actor, UUID mosqueId, EventDetails details) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        Event event = events.save(Event.draft(mosqueId, details, actor.id(), Instant.now(clock)));
        return views.of(event, null);
    }

    @Transactional
    public EventView update(AuthenticatedUser actor, UUID mosqueId, UUID eventId, EventDetails details) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        Event event = load(mosqueId, eventId);
        long confirmed = registrations.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED);
        if (details.capacity() != null && details.capacity() < confirmed) {
            throw new EventRuleViolationException("CAPACITY_BELOW_REGISTRATIONS",
                    "Capacity cannot be lower than the " + confirmed + " confirmed registrations.");
        }
        event.update(details, actor.id(), Instant.now(clock));
        return views.of(event, null);
    }

    @Transactional
    public EventView publish(AuthenticatedUser actor, UUID mosqueId, UUID eventId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        Event event = load(mosqueId, eventId);
        event.publish(actor.id(), Instant.now(clock));
        return views.of(event, null);
    }

    /** Back to draft. Refused while people are registered: cancel the event instead, so they see why. */
    @Transactional
    public EventView unpublish(AuthenticatedUser actor, UUID mosqueId, UUID eventId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        Event event = load(mosqueId, eventId);
        if (registrations.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED) > 0) {
            throw new EventHasRegistrationsException("Cancel the event instead: people are registered for it.");
        }
        event.unpublish(actor.id(), Instant.now(clock));
        return views.of(event, null);
    }

    @Transactional
    public EventView cancel(AuthenticatedUser actor, UUID mosqueId, UUID eventId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        Event event = load(mosqueId, eventId);
        event.cancel(actor.id(), Instant.now(clock));
        return views.of(event, null);
    }

    /** Only drafts without any registration history can be deleted; published events are cancelled instead. */
    @Transactional
    public void delete(AuthenticatedUser actor, UUID mosqueId, UUID eventId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        Event event = load(mosqueId, eventId);
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new EventRuleViolationException("EVENT_NOT_DRAFT",
                    "Only a draft can be deleted. Unpublish or cancel the event instead.");
        }
        if (registrations.existsByEventId(eventId)) {
            throw new EventHasRegistrationsException("This event has registrations and cannot be deleted.");
        }
        events.delete(event);
    }

    /**
     * Sends a push notification about a published event that has not ended to the followers who want events. By
     * default the title, and the date, time and location in the mosque's time zone ("za 14 nov, 18:30 · Grote zaal").
     */
    @Transactional
    public PushNotificationView sendPush(AuthenticatedUser actor, UUID mosqueId, UUID eventId, PushText text) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        Event event = load(mosqueId, eventId);
        EventDetails details = event.details();
        if (event.getStatus() != EventStatus.PUBLISHED || event.isOver(Instant.now(clock))) {
            throw new PushContentNotPublishedException("Only a published event that has not ended can be sent as a "
                    + "push notification.");
        }
        ZoneId zone = ZoneId.of(mosques.requireActive(mosqueId).timeZone());
        return push.send(actor, new PushRequest(mosqueId, PushContentType.EVENT, eventId,
                text.titleOr(details.title()), text.bodyOr(defaultPushBody(details, zone))));
    }

    static String defaultPushBody(EventDetails details, ZoneId zone) {
        // Java's Dutch abbreviations end with a dot ("za.", "nov."); the app and portal write them without.
        String when = PUSH_DATE.format(details.startsAt().atZone(zone)).replace(".", "");
        return details.location() == null ? when : when + " · " + details.location();
    }

    private Event load(UUID mosqueId, UUID eventId) {
        return events.findByIdAndMosqueId(eventId, mosqueId).orElseThrow(EventNotFound::exception);
    }

    static class EventHasRegistrationsException extends ApplicationException {

        EventHasRegistrationsException(String message) {
            super(ErrorCategory.CONFLICT, "EVENT_HAS_REGISTRATIONS", message);
        }
    }
}
