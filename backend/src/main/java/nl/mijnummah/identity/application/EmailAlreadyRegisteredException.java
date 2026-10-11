package nl.mijnummah.identity.application;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/** Another account already uses this email address. */
public class EmailAlreadyRegisteredException extends ApplicationException {

    public EmailAlreadyRegisteredException() {
        super(ErrorCategory.CONFLICT, "EMAIL_ALREADY_REGISTERED", "An account with this email address already exists.");
    }
}
