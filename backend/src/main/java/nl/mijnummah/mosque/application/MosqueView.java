package nl.mijnummah.mosque.application;

import java.time.Instant;
import java.util.UUID;

import nl.mijnummah.mosque.MosqueRole;
import nl.mijnummah.mosque.MosqueStatus;
import nl.mijnummah.mosque.domain.Mosque;
import nl.mijnummah.mosque.domain.MosqueDetails;

/**
 * A mosque as returned by the application layer.
 *
 * @param logoImageId  null when the mosque has no logo
 * @param coverImageId null when the mosque has no cover photo
 * @param myRole the caller's role for this mosque in admin views; null for platform administrators without a
 *               role of their own and in public views
 */
public record MosqueView(UUID id, String slug, MosqueDetails details, MosqueStatus status, UUID logoImageId,
                         UUID coverImageId, Instant createdAt, Instant updatedAt, MosqueRole myRole) {

    static MosqueView of(Mosque mosque, MosqueRole myRole) {
        return new MosqueView(mosque.getId(), mosque.getSlug(), mosque.details(), mosque.getStatus(),
                mosque.getLogoImageId(), mosque.getCoverImageId(), mosque.getCreatedAt(), mosque.getUpdatedAt(),
                myRole);
    }
}
