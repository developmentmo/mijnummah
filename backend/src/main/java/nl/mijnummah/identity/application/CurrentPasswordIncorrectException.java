package nl.mijnummah.identity.application;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/**
 * The current password given to confirm a change was wrong. A 400 rather than a 401: the session itself is valid,
 * and clients treat 401 as "sign in again".
 */
public class CurrentPasswordIncorrectException extends ApplicationException {

    public CurrentPasswordIncorrectException() {
        super(ErrorCategory.INVALID_REQUEST, "CURRENT_PASSWORD_INCORRECT", "The current password is incorrect.");
    }
}
