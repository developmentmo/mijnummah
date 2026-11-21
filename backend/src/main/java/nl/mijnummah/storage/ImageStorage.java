package nl.mijnummah.storage;

import java.util.UUID;

/** Uploaded images, for other modules. */
public interface ImageStorage {

    /**
     * Checks, decodes and re-encodes an uploaded image and stores it for a mosque. The caller has already checked
     * that the uploader may manage content of this mosque.
     *
     * @throws InvalidImageException when the file is not a JPEG or PNG image, too large or too small
     */
    StoredImageRef store(UUID mosqueId, UUID uploaderId, byte[] content);

    /** @throws ImageNotFoundException when the image does not exist or belongs to another mosque */
    void requireOwnedBy(UUID imageId, UUID mosqueId);

    /** Public URL path of an image, relative to the API base URL. */
    static String publicPath(UUID imageId) {
        return "/api/v1/public/images/" + imageId;
    }
}
