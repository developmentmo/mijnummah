package nl.mijnummah.storage;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

public class InvalidImageException extends ApplicationException {

    public InvalidImageException(String message) {
        super(ErrorCategory.INVALID_REQUEST, "INVALID_IMAGE", message);
    }
}
