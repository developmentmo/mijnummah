package nl.mijnummah.event.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import nl.mijnummah.common.web.PageResponse;
import nl.mijnummah.event.application.EventView;
import nl.mijnummah.mosque.MosqueCard;
import nl.mijnummah.mosque.MosqueDirectory;
import nl.mijnummah.mosque.MosqueRef;

/** Adds the mosque (name, slug, logo) to public events, so the central app can show where an event is. */
@Component
class PublicEventResponses {

    private final MosqueDirectory mosques;

    PublicEventResponses(MosqueDirectory mosques) {
        this.mosques = mosques;
    }

    PageResponse<EventResponse> page(Page<EventView> page) {
        Set<UUID> ids = page.getContent().stream().map(EventView::mosqueId).collect(Collectors.toSet());
        Map<UUID, MosqueRef> refs = mosques.findActiveByIds(ids);
        Map<UUID, Boolean> accepting = ids.stream().collect(Collectors.toMap(Function.identity(),
                id -> mosques.settings(id).registrationsEnabled()));
        return PageResponse.of(page, view -> EventResponse.from(view, MosqueCard.of(refs.get(view.mosqueId())),
                accepting.get(view.mosqueId())));
    }

    /** Registration shows as disabled when the platform switched registrations off for the mosque. */
    EventResponse one(EventView view) {
        MosqueRef ref = mosques.findActiveByIds(List.of(view.mosqueId())).get(view.mosqueId());
        return EventResponse.from(view, MosqueCard.of(ref), mosques.settings(view.mosqueId()).registrationsEnabled());
    }
}
