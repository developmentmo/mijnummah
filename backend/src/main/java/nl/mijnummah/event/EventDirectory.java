package nl.mijnummah.event;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Read-only lookup of events for other modules (e.g. linking a recording to the lecture it came from). */
public interface EventDirectory {

    boolean existsInMosque(UUID eventId, UUID mosqueId);

    /** Published or cancelled events among the given IDs, regardless of visibility. */
    Map<UUID, EventSummary> findPublicSummaries(Collection<UUID> eventIds);
}
