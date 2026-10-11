package nl.mijnummah.media.application;

import nl.mijnummah.common.error.NotFoundException;

final class MediaNotFound {

    private MediaNotFound() {
    }

    static NotFoundException exception() {
        return new NotFoundException("MEDIA_NOT_FOUND", "Media item not found.");
    }
}
