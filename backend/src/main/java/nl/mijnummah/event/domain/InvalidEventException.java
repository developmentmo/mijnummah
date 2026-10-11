package nl.mijnummah.event.domain;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

public class InvalidEventException extends ApplicationException {

    public InvalidEventException(String message) {
        super(ErrorCategory.INVALID_REQUEST, "INVALID_EVENT", message);
    }
}
