package nl.mijnummah.announcement.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import nl.mijnummah.announcement.application.AnnouncementView;
import nl.mijnummah.common.web.PageResponse;
import nl.mijnummah.mosque.MosqueCard;
import nl.mijnummah.mosque.MosqueDirectory;
import nl.mijnummah.mosque.MosqueRef;

/** Adds the mosque (name, slug, logo) to public announcements. */
@Component
class PublicAnnouncementResponses {

    private final MosqueDirectory mosques;

    PublicAnnouncementResponses(MosqueDirectory mosques) {
        this.mosques = mosques;
    }

    PageResponse<AnnouncementResponse> page(Page<AnnouncementView> page) {
        Map<UUID, MosqueRef> refs = mosques.findActiveByIds(page.getContent().stream()
                .map(AnnouncementView::mosqueId).collect(Collectors.toSet()));
        return PageResponse.of(page, view -> AnnouncementResponse.from(view, MosqueCard.of(refs.get(view.mosqueId()))));
    }

    AnnouncementResponse one(AnnouncementView view) {
        MosqueRef ref = mosques.findActiveByIds(List.of(view.mosqueId())).get(view.mosqueId());
        return AnnouncementResponse.from(view, MosqueCard.of(ref));
    }
}
