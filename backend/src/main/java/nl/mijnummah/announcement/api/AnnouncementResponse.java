package nl.mijnummah.announcement.api;

import java.time.Instant;
import java.util.UUID;

import nl.mijnummah.announcement.application.AnnouncementView;
import nl.mijnummah.announcement.domain.AnnouncementStatus;

record AnnouncementResponse(UUID id, String title, String body, boolean featured, Instant publishedAt,
                            Instant expiresAt) {

    static AnnouncementResponse from(AnnouncementView view) {
        var d = view.details();
        return new AnnouncementResponse(view.id(), d.title(), d.body(), d.featured(), view.publishedAt(), d.expiresAt());
    }

    record Admin(UUID id, UUID mosqueId, String title, String body, boolean featured, AnnouncementStatus status,
                 boolean expired, Instant publishedAt, Instant expiresAt, Instant createdAt, Instant updatedAt) {

        static Admin from(AnnouncementView view) {
            var d = view.details();
            return new Admin(view.id(), view.mosqueId(), d.title(), d.body(), d.featured(), view.status(),
                    view.expired(), view.publishedAt(), d.expiresAt(), view.createdAt(), view.updatedAt());
        }
    }
}
