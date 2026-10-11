package nl.mijnummah.event.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;
import nl.mijnummah.common.error.NotFoundException;
import nl.mijnummah.event.domain.Event;
import nl.mijnummah.event.domain.EventRegistration;
import nl.mijnummah.event.domain.EventRuleViolationException;
import nl.mijnummah.event.domain.EventStatus;
import nl.mijnummah.event.domain.RegistrationStatus;
import nl.mijnummah.event.infrastructure.EventRegistrationRepository;
import nl.mijnummah.event.infrastructure.EventRepository;
import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.identity.UserDirectory;
import nl.mijnummah.identity.UserSummary;
import nl.mijnummah.mosque.MosqueAccessService;
import nl.mijnummah.mosque.MosqueDirectory;
import nl.mijnummah.mosque.MosquePermission;
import nl.mijnummah.mosque.MosqueRef;

/**
 * Registrations for events by signed-in users, and the registration list for a mosque's administrators.
 * <p>
 * A user can register for an event they can see publicly, that is published, has registration enabled, has not
 * started yet and has a free place. The event row is locked while registering, so the capacity holds under
 * concurrent requests.
 */
@Service
public class RegistrationService {

    private final EventRepository events;
    private final EventRegistrationRepository registrations;
    private final MosqueDirectory mosques;
    private final MosqueAccessService access;
    private final UserDirectory users;
    private final Clock clock;

    public RegistrationService(EventRepository events, EventRegistrationRepository registrations,
                               MosqueDirectory mosques, MosqueAccessService access, UserDirectory users, Clock clock) {
        this.events = events;
        this.registrations = registrations;
        this.mosques = mosques;
        this.access = access;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public RegistrationView register(AuthenticatedUser actor, UUID eventId) {
        Instant now = Instant.now(clock);
        Event event = events.findByIdForUpdate(eventId).orElseThrow(EventNotFound::exception);
        MosqueRef mosque = visibleMosque(event, actor);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new EventRuleViolationException("EVENT_CANCELLED", "This event has been cancelled.");
        }
        if (!event.isRegistrationEnabled()) {
            throw new EventRuleViolationException("REGISTRATION_NOT_ENABLED",
                    "Registration is not possible for this event.");
        }
        if (event.hasStarted(now)) {
            throw new EventRuleViolationException("REGISTRATION_CLOSED", "This event has already started.");
        }
        Optional<EventRegistration> existing = registrations.findByEventIdAndUserId(eventId, actor.id());
        if (existing.filter(EventRegistration::isConfirmed).isPresent()) {
            throw new RegistrationConflictException("ALREADY_REGISTERED", "You are already registered for this event.");
        }
        if (event.getCapacity() != null
                && registrations.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED) >= event.getCapacity()) {
            throw new RegistrationConflictException("EVENT_FULL", "This event is full.");
        }
        EventRegistration registration = existing.orElse(null);
        if (registration != null) {
            registration.reconfirm(now);
        } else {
            registration = EventRegistration.confirm(event, actor.id(), now);
            try {
                registrations.saveAndFlush(registration);
            } catch (DataIntegrityViolationException ex) {
                throw new RegistrationConflictException("ALREADY_REGISTERED",
                        "You are already registered for this event.");
            }
        }
        return view(registration, event, mosque);
    }

    @Transactional
    public void cancel(AuthenticatedUser actor, UUID eventId) {
        Event event = events.findById(eventId).orElseThrow(EventNotFound::exception);
        visibleMosque(event, actor);
        EventRegistration registration = registrations.findByEventIdAndUserId(eventId, actor.id())
                .filter(EventRegistration::isConfirmed)
                .orElseThrow(() -> new NotFoundException("REGISTRATION_NOT_FOUND",
                        "You are not registered for this event."));
        Instant now = Instant.now(clock);
        if (event.hasStarted(now)) {
            throw new EventRuleViolationException("REGISTRATION_CLOSED", "This event has already started.");
        }
        registration.cancel(now);
    }

    /** The caller's confirmed registrations, newest first. Events of deactivated mosques are left out. */
    @Transactional(readOnly = true)
    public Page<RegistrationView> mine(AuthenticatedUser actor, Pageable pageable) {
        Page<EventRegistration> page = registrations.findByUserIdAndStatus(actor.id(), RegistrationStatus.CONFIRMED,
                pageable);
        Map<UUID, Event> eventsById = events.findAllById(page.getContent().stream()
                        .map(EventRegistration::getEventId).toList()).stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));
        Map<UUID, MosqueRef> mosquesById = mosques.findActiveByIds(page.getContent().stream()
                .map(EventRegistration::getMosqueId).distinct().toList());
        List<RegistrationView> content = page.getContent().stream()
                .filter(registration -> mosquesById.containsKey(registration.getMosqueId()))
                .map(registration -> view(registration, eventsById.get(registration.getEventId()),
                        mosquesById.get(registration.getMosqueId())))
                .toList();
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    /** Everyone who registered for an event (confirmed and cancelled). Requires VIEW_REGISTRATIONS. */
    @Transactional(readOnly = true)
    public List<RegistrantView> registrants(AuthenticatedUser actor, UUID mosqueId, UUID eventId) {
        access.require(actor, mosqueId, MosquePermission.VIEW_REGISTRATIONS);
        if (!events.existsByIdAndMosqueId(eventId, mosqueId)) {
            throw EventNotFound.exception();
        }
        List<EventRegistration> rows = registrations.findByEventIdAndMosqueIdOrderByCreatedAt(eventId, mosqueId);
        Map<UUID, UserSummary> people = users.findByIds(rows.stream().map(EventRegistration::getUserId).toList());
        return rows.stream().map(row -> {
            UserSummary person = people.get(row.getUserId());
            return new RegistrantView(row.getId(), row.getUserId(), person == null ? null : person.displayName(),
                    person == null ? null : person.email(), row.getStatus(), row.getCreatedAt(), row.getUpdatedAt());
        }).toList();
    }

    /** The event must be publicly visible to this user, in an active mosque; otherwise it does not exist for them. */
    private MosqueRef visibleMosque(Event event, AuthenticatedUser actor) {
        if (!PublicEventService.isVisible(event, actor)) {
            throw EventNotFound.exception();
        }
        MosqueRef mosque = mosques.findActiveByIds(List.of(event.getMosqueId())).get(event.getMosqueId());
        if (mosque == null) {
            throw EventNotFound.exception();
        }
        return mosque;
    }

    private static RegistrationView view(EventRegistration registration, Event event, MosqueRef mosque) {
        return new RegistrationView(registration.getId(), event.getId(), event.getTitle(), event.getStartsAt(),
                mosque.id(), mosque.slug(), mosque.name(), registration.getStatus(), registration.getUpdatedAt());
    }

    static class RegistrationConflictException extends ApplicationException {

        RegistrationConflictException(String code, String message) {
            super(ErrorCategory.CONFLICT, code, message);
        }
    }
}
