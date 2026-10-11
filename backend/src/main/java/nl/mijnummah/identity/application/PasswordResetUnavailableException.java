package nl.mijnummah.identity.application;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/** Email is not configured, so no reset code can be sent. Same answer for every address. */
public class PasswordResetUnavailableException extends ApplicationException {

    public PasswordResetUnavailableException() {
        super(ErrorCategory.UNAVAILABLE, "PASSWORD_RESET_UNAVAILABLE",
                "Password reset by email is not available at the moment.");
    }
}
