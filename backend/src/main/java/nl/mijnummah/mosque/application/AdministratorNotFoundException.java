package nl.mijnummah.mosque.application;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/** No administrator with this ID for this mosque. */
public class AdministratorNotFoundException extends ApplicationException {

    public AdministratorNotFoundException() {
        super(ErrorCategory.NOT_FOUND, "ADMINISTRATOR_NOT_FOUND", "Administrator not found for this mosque.");
    }
}
