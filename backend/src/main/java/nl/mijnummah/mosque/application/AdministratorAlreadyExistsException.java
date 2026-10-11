package nl.mijnummah.mosque.application;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/** One administrator row per user and mosque; change the role instead. */
public class AdministratorAlreadyExistsException extends ApplicationException {

    public AdministratorAlreadyExistsException() {
        super(ErrorCategory.CONFLICT, "ADMINISTRATOR_ALREADY_EXISTS", "This user is already an administrator of this mosque.");
    }
}
