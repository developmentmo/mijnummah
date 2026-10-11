package nl.mijnummah.event.api;

import java.time.Instant;
import java.util.UUID;

import nl.mijnummah.event.application.EventView;
import nl.mijnummah.event.domain.EventCategory;
import nl.mijnummah.event.domain.EventStatus;
import nl.mijnummah.event.domain.Visibility;

/**
 * An event. {@code status} is PUBLISHED or CANCELLED in public responses.
 *
 * @param registration whether and how people can register
 */
record EventResponse(UUID id, String title, String description, EventCategory category, Instant startsAt,
                     Instant endsAt, String location, String imageUrl, Visibility visibility, EventStatus status,
                     Registration registration) {

    /**
     * @param availablePlaces remaining places, null without a capacity limit
     * @param registeredByMe  whether the signed-in caller is registered; null for anonymous callers
     */
    record Registration(boolean enabled, Integer capacity, Integer availablePlaces, Boolean registeredByMe) {
    }

    static EventResponse from(EventView view) {
        var d = view.details();
        return new EventResponse(view.id(), d.title(), d.description(), d.category(), d.startsAt(), d.endsAt(),
                d.location(), d.imageUrl(), d.visibility(), view.status(),
                new Registration(d.registrationEnabled(), d.capacity(), view.availablePlaces(), view.registeredByMe()));
    }

    /** Admin view: adds lifecycle and registration figures. */
    record Admin(UUID id, UUID mosqueId, String title, String description, EventCategory category, Instant startsAt,
                 Instant endsAt, String location, String imageUrl, Visibility visibility, boolean registrationEnabled,
                 Integer capacity, long confirmedRegistrations, Integer availablePlaces, EventStatus status,
                 Instant publishedAt, Instant cancelledAt, Instant createdAt, Instant updatedAt) {

        static Admin from(EventView view) {
            var d = view.details();
            return new Admin(view.id(), view.mosqueId(), d.title(), d.description(), d.category(), d.startsAt(),
                    d.endsAt(), d.location(), d.imageUrl(), d.visibility(), d.registrationEnabled(), d.capacity(),
                    view.confirmedRegistrations(), view.availablePlaces(), view.status(), view.publishedAt(),
                    view.cancelledAt(), view.createdAt(), view.updatedAt());
        }
    }
}
