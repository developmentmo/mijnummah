package nl.mijnummah.mosque;

import java.util.UUID;

/** Identity of an active mosque, for other modules. */
public record MosqueRef(UUID id, String slug, String name, String timeZone) {
}
