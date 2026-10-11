package nl.mijnummah.identity.application;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/** No account with this ID. */
public class UserNotFoundException extends ApplicationException {

    public UserNotFoundException() {
        super(ErrorCategory.NOT_FOUND, "USER_NOT_FOUND", "User not found.");
    }
}
