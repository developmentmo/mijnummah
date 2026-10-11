package nl.mijnummah.event.application;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import nl.mijnummah.event.domain.Event;
import nl.mijnummah.event.infrastructure.EventRegistrationRepository;

/** Builds {@link EventView}s with registration figures in two queries per page, not one per event. */
@Component
class EventViews {

    private final EventRegistrationRepository registrations;

    EventViews(EventRegistrationRepository registrations) {
        this.registrations = registrations;
    }

    List<EventView> of(Collection<Event> events, UUID viewerId) {
        if (events.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = events.stream().map(Event::getId).toList();
        Map<UUID, Long> confirmed = registrations.countConfirmed(ids).stream()
                .collect(Collectors.toMap(EventRegistrationRepository.ConfirmedCount::getEventId,
                        EventRegistrationRepository.ConfirmedCount::getConfirmed));
        Set<UUID> mine = viewerId == null ? Set.of() : Set.copyOf(registrations.confirmedEventIdsOf(viewerId, ids));
        return events.stream()
                .map(event -> EventView.of(event, confirmed.getOrDefault(event.getId(), 0L),
                        viewerId == null ? null : mine.contains(event.getId())))
                .toList();
    }

    EventView of(Event event, UUID viewerId) {
        return of(List.of(event), viewerId).getFirst();
    }
}
