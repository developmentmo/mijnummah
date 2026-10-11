package nl.mijnummah.event.application;

import nl.mijnummah.common.error.NotFoundException;

final class EventNotFound {

    private EventNotFound() {
    }

    static NotFoundException exception() {
        return new NotFoundException("EVENT_NOT_FOUND", "Event not found.");
    }
}
