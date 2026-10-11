package nl.mijnummah.identity.application;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/** Correct credentials, but the account is blocked. */
public class AccountBlockedException extends ApplicationException {

    public AccountBlockedException() {
        super(ErrorCategory.FORBIDDEN, "ACCOUNT_BLOCKED", "This account has been blocked.");
    }
}
