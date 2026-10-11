package nl.mijnummah.event.application;

import java.time.Instant;
import java.util.UUID;

import nl.mijnummah.event.domain.RegistrationStatus;

/** A registration as seen by the mosque's administrators (contains personal data). */
public record RegistrantView(UUID registrationId, UUID userId, String displayName, String email,
                             RegistrationStatus status, Instant registeredAt, Instant updatedAt) {
}
