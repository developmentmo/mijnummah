package nl.mijnummah.media.domain;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

public class InvalidMediaException extends ApplicationException {

    public InvalidMediaException(String code, String message) {
        super(ErrorCategory.INVALID_REQUEST, code, message);
    }
}
