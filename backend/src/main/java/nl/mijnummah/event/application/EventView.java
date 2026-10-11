package nl.mijnummah.event.application;

import java.time.Instant;
import java.util.UUID;

import nl.mijnummah.event.domain.Event;
import nl.mijnummah.event.domain.EventDetails;
import nl.mijnummah.event.domain.EventStatus;

/**
 * An event with its registration figures.
 *
 * @param registeredByMe whether the signed-in caller has a confirmed registration; null for anonymous callers and
 *                       in admin views
 */
public record EventView(UUID id, UUID mosqueId, EventDetails details, EventStatus status, Instant publishedAt,
                        Instant cancelledAt, Instant createdAt, Instant updatedAt, long confirmedRegistrations,
                        Boolean registeredByMe) {

    static EventView of(Event event, long confirmedRegistrations, Boolean registeredByMe) {
        return new EventView(event.getId(), event.getMosqueId(), event.details(), event.getStatus(),
                event.getPublishedAt(), event.getCancelledAt(), event.getCreatedAt(), event.getUpdatedAt(),
                confirmedRegistrations, registeredByMe);
    }

    /** Remaining places, or null when the event has no capacity limit. */
    public Integer availablePlaces() {
        Integer capacity = details.capacity();
        return capacity == null ? null : (int) Math.max(0, capacity - confirmedRegistrations);
    }
}
