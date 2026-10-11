package nl.mijnummah.mosque.domain;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

public class InvalidMosqueException extends ApplicationException {

    public InvalidMosqueException(String message) {
        super(ErrorCategory.INVALID_REQUEST, "INVALID_MOSQUE", message);
    }
}
