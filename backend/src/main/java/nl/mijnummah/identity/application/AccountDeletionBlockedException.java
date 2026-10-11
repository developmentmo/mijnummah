package nl.mijnummah.identity.application;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/** Accounts that administer a mosque or authored content are removed by a platform administrator, not by self-service. */
public class AccountDeletionBlockedException extends ApplicationException {

    public AccountDeletionBlockedException() {
        super(ErrorCategory.CONFLICT, "ACCOUNT_DELETION_BLOCKED",
                "This account manages a mosque and cannot be deleted here. Contact the platform administrator.");
    }
}
