package nl.mijnummah.mosque.application;

import java.time.Instant;
import java.util.UUID;

import nl.mijnummah.mosque.MosqueRole;
import nl.mijnummah.mosque.domain.Mosque;
import nl.mijnummah.mosque.domain.MosqueDetails;

/**
 * A mosque as returned by the application layer.
 *
 * @param myRole the caller's role for this mosque in admin views; null for platform administrators without a
 *               role of their own and in public views
 */
public record MosqueView(UUID id, String slug, MosqueDetails details, boolean active, Instant createdAt,
                         Instant updatedAt, MosqueRole myRole) {

    static MosqueView of(Mosque mosque, MosqueRole myRole) {
        return new MosqueView(mosque.getId(), mosque.getSlug(), mosque.details(), mosque.isActive(),
                mosque.getCreatedAt(), mosque.getUpdatedAt(), myRole);
    }
}
