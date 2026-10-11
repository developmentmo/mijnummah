package nl.mijnummah.identity.domain;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

public class InvalidPasswordException extends ApplicationException {

    public InvalidPasswordException(String message) {
        super(ErrorCategory.INVALID_REQUEST, "INVALID_PASSWORD", message);
    }
}
