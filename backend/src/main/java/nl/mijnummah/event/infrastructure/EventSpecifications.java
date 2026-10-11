package nl.mijnummah.event.infrastructure;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import nl.mijnummah.event.domain.Event;
import nl.mijnummah.event.domain.EventCategory;
import nl.mijnummah.event.domain.EventStatus;
import nl.mijnummah.event.domain.Visibility;

/** Composable filters for event lists. A null argument means "no filter". */
public final class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<Event> ofMosque(UUID mosqueId) {
        return (root, query, cb) -> cb.equal(root.get("mosqueId"), mosqueId);
    }

    public static Specification<Event> statusIn(Collection<EventStatus> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<Event> visibilityIn(Collection<Visibility> visibilities) {
        return (root, query, cb) -> root.get("visibility").in(visibilities);
    }

    public static Specification<Event> category(EventCategory category) {
        return (root, query, cb) -> category == null ? null : cb.equal(root.get("category"), category);
    }

    /** Not yet over at {@code from}: ends at or after it, or (without an end) starts at or after it. */
    public static Specification<Event> notOverBefore(Instant from) {
        return (root, query, cb) -> from == null ? null
                : cb.greaterThanOrEqualTo(cb.coalesce(root.get("endsAt"), root.get("startsAt")), from);
    }

    public static Specification<Event> startsBefore(Instant to) {
        return (root, query, cb) -> to == null ? null : cb.lessThan(root.get("startsAt"), to);
    }
}
