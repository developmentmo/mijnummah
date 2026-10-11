package nl.mijnummah.identity.application;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/** Deliberately does not reveal whether the email exists. */
public class InvalidCredentialsException extends ApplicationException {

    public InvalidCredentialsException() {
        super(ErrorCategory.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect.");
    }
}
