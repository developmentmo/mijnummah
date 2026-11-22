package nl.mijnummah.mosque;

import java.util.UUID;

import nl.mijnummah.storage.ImageStorage;

/**
 * The mosque a content item belongs to, as shown next to it in the central app (D-060).
 *
 * @param logoUrl path of the logo relative to the API base URL; null when there is none
 */
public record MosqueCard(UUID id, String slug, String name, String logoUrl) {

    public static MosqueCard of(MosqueRef mosque) {
        return mosque == null ? null : new MosqueCard(mosque.id(), mosque.slug(), mosque.name(),
                mosque.logoImageId() == null ? null : ImageStorage.publicPath(mosque.logoImageId()));
    }
}
