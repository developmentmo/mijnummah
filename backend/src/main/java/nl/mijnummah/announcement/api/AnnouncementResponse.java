package nl.mijnummah.announcement.api;

import java.time.Instant;
import java.util.UUID;

import nl.mijnummah.announcement.application.AnnouncementView;
import nl.mijnummah.announcement.domain.AnnouncementDetails;
import nl.mijnummah.announcement.domain.AnnouncementStatus;
import nl.mijnummah.mosque.MosqueCard;
import nl.mijnummah.storage.ImageStorage;

/** An announcement; {@code imageUrl} is the path of its image from the API base, or null. */
record AnnouncementResponse(UUID id, MosqueCard mosque, String title, String body, boolean featured,
                            String imageUrl, Instant publishedAt, Instant expiresAt) {

    static AnnouncementResponse from(AnnouncementView view, MosqueCard mosque) {
        var d = view.details();
        return new AnnouncementResponse(view.id(), mosque, d.title(), d.body(), d.featured(), imageUrl(d), view.publishedAt(),
                d.expiresAt());
    }

    static String imageUrl(AnnouncementDetails details) {
        return details.imageId() == null ? null : ImageStorage.publicPath(details.imageId());
    }

    record Admin(UUID id, UUID mosqueId, String title, String body, boolean featured, String imageUrl, UUID imageId,
                 AnnouncementStatus status, boolean expired, Instant publishedAt, Instant expiresAt,
                 Instant createdAt, Instant updatedAt) {

        static Admin from(AnnouncementView view) {
            var d = view.details();
            return new Admin(view.id(), view.mosqueId(), d.title(), d.body(), d.featured(), AnnouncementResponse.imageUrl(d), d.imageId(),
                    view.status(), view.expired(), view.publishedAt(), d.expiresAt(), view.createdAt(),
                    view.updatedAt());
        }
    }
}
