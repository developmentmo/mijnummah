package nl.mijnummah.event.application;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.event.EventDirectory;
import nl.mijnummah.event.EventSummary;
import nl.mijnummah.event.domain.Event;
import nl.mijnummah.event.infrastructure.EventRepository;

@Service
class EventDirectoryService implements EventDirectory {

    private final EventRepository events;

    EventDirectoryService(EventRepository events) {
        this.events = events;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInMosque(UUID eventId, UUID mosqueId) {
        return events.existsByIdAndMosqueId(eventId, mosqueId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, EventSummary> findPublicSummaries(Collection<UUID> eventIds) {
        return events.findAllById(eventIds).stream()
                .filter(Event::isPubliclyListed)
                .map(event -> new EventSummary(event.getId(), event.getTitle(), event.getStartsAt()))
                .collect(Collectors.toUnmodifiableMap(EventSummary::id, Function.identity()));
    }
}
