package nl.mijnummah.identity.application;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/** Prevents a platform administrator from locking themselves out. */
public class CannotChangeOwnAccountException extends ApplicationException {

    public CannotChangeOwnAccountException() {
        super(ErrorCategory.BUSINESS_RULE_VIOLATION, "CANNOT_CHANGE_OWN_ACCOUNT", "You cannot block your own account.");
    }
}
