package nl.mijnummah.mosque;

import java.util.UUID;

/**
 * Identity of an active mosque, for other modules.
 *
 * @param logoImageId the uploaded logo; null when the mosque has none
 */
public record MosqueRef(UUID id, String slug, String name, String timeZone, UUID logoImageId) {
}
