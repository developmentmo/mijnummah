package nl.mijnummah.mosque.application;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/** The email address given for a new administrator has no active account. */
public class AdministratorUserNotFoundException extends ApplicationException {

    public AdministratorUserNotFoundException() {
        super(ErrorCategory.NOT_FOUND, "USER_NOT_FOUND", "No active account with this email address. A platform administrator can create one.");
    }
}
