package nl.mijnummah.announcement.api;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import nl.mijnummah.announcement.domain.AnnouncementDetails;

record AnnouncementRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 5000) String body,
        Boolean featured,
        Instant expiresAt) {

    AnnouncementDetails toDetails() {
        return new AnnouncementDetails(title, body, Boolean.TRUE.equals(featured), expiresAt);
    }
}
