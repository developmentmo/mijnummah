package nl.mijnummah.storage;

import nl.mijnummah.common.error.NotFoundException;

public class ImageNotFoundException extends NotFoundException {

    public ImageNotFoundException() {
        super("IMAGE_NOT_FOUND", "Image not found.");
    }
}
