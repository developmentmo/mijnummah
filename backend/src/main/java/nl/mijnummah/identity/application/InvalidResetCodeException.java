package nl.mijnummah.identity.application;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/** Wrong, expired, used or used-up code, or an unknown address: one answer for all, so nothing is revealed. */
public class InvalidResetCodeException extends ApplicationException {

    public InvalidResetCodeException() {
        super(ErrorCategory.INVALID_REQUEST, "INVALID_RESET_CODE",
                "The code is incorrect or no longer valid. Request a new code.");
    }
}
