package nl.mijnummah.mosque.application;

import nl.mijnummah.common.error.NotFoundException;

final class MosqueNotFound {

    private MosqueNotFound() {
    }

    static NotFoundException exception() {
        return new NotFoundException("MOSQUE_NOT_FOUND", "Mosque not found.");
    }
}
