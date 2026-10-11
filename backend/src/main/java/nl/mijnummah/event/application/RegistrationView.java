package nl.mijnummah.event.application;

import java.time.Instant;
import java.util.UUID;

import nl.mijnummah.event.domain.RegistrationStatus;

/** A registration with what the user needs to recognise the event and the mosque. */
public record RegistrationView(UUID id, UUID eventId, String eventTitle, Instant eventStartsAt, UUID mosqueId,
                               String mosqueSlug, String mosqueName, RegistrationStatus status, Instant registeredAt) {
}
