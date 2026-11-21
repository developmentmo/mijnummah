package nl.mijnummah.storage;

import java.util.UUID;

/** A stored image: its ID, public path and size in pixels. */
public record StoredImageRef(UUID id, String path, int width, int height) {
}
