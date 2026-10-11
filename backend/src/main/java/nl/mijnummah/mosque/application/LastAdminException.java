package nl.mijnummah.mosque.application;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/** Prevents a mosque from ending up without anyone who can manage it. */
public class LastAdminException extends ApplicationException {

    public LastAdminException() {
        super(ErrorCategory.BUSINESS_RULE_VIOLATION, "LAST_ADMIN", "A mosque must keep at least one administrator with role ADMIN.");
    }
}
