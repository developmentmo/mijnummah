package nl.mijnummah.media.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import nl.mijnummah.common.web.PageResponse;
import nl.mijnummah.media.application.MediaView;
import nl.mijnummah.mosque.MosqueCard;
import nl.mijnummah.mosque.MosqueDirectory;
import nl.mijnummah.mosque.MosqueRef;

/** Adds the mosque (name, slug, logo) to public media items, so the central app can show whose video it is. */
@Component
class PublicMediaResponses {

    private final MosqueDirectory mosques;

    PublicMediaResponses(MosqueDirectory mosques) {
        this.mosques = mosques;
    }

    PageResponse<MediaResponse> page(Page<MediaView> page) {
        Map<UUID, MosqueRef> refs = refs(page.getContent());
        return PageResponse.of(page, view -> MediaResponse.from(view, MosqueCard.of(refs.get(view.mosqueId()))));
    }

    List<MediaResponse> list(List<MediaView> views) {
        Map<UUID, MosqueRef> refs = refs(views);
        return views.stream().map(view -> MediaResponse.from(view, MosqueCard.of(refs.get(view.mosqueId())))).toList();
    }

    MediaResponse one(MediaView view) {
        return list(List.of(view)).getFirst();
    }

    private Map<UUID, MosqueRef> refs(Collection<MediaView> views) {
        return mosques.findActiveByIds(views.stream().map(MediaView::mosqueId).collect(Collectors.toSet()));
    }
}
