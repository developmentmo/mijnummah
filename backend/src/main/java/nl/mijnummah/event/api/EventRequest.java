package nl.mijnummah.event.api;

import java.time.Instant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import nl.mijnummah.event.domain.EventCategory;
import nl.mijnummah.event.domain.EventDetails;
import nl.mijnummah.event.domain.Visibility;

/**
 * Create or replace an event. Times are ISO-8601 instants with offset, e.g. 2026-11-06T18:30:00+01:00. Omitted
 * booleans are false; an omitted visibility is PUBLIC.
 */
record EventRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 10000) String description,
        @NotNull EventCategory category,
        @NotNull Instant startsAt,
        Instant endsAt,
        @Size(max = 200) String location,
        @Size(max = 500) @Pattern(regexp = "^https://\\S+$", message = "must be an https URL") String imageUrl,
        Visibility visibility,
        Boolean registrationEnabled,
        @Min(1) Integer capacity) {

    EventDetails toDetails() {
        return new EventDetails(title, description, category, startsAt, endsAt, location, imageUrl, visibility,
                Boolean.TRUE.equals(registrationEnabled), capacity);
    }
}
