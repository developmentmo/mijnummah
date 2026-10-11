package nl.mijnummah.announcement.application;

import java.time.Instant;
import java.util.UUID;

import nl.mijnummah.announcement.domain.Announcement;
import nl.mijnummah.announcement.domain.AnnouncementDetails;
import nl.mijnummah.announcement.domain.AnnouncementStatus;

public record AnnouncementView(UUID id, UUID mosqueId, AnnouncementDetails details, AnnouncementStatus status,
                               boolean expired, Instant publishedAt, Instant createdAt, Instant updatedAt) {

    static AnnouncementView of(Announcement announcement, Instant now) {
        return new AnnouncementView(announcement.getId(), announcement.getMosqueId(), announcement.details(),
                announcement.getStatus(), announcement.isExpired(now), announcement.getPublishedAt(),
                announcement.getCreatedAt(), announcement.getUpdatedAt());
    }
}
