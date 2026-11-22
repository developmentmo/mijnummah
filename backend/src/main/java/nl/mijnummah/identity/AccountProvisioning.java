package nl.mijnummah.identity;

import java.util.UUID;

/**
 * Accounts for mosque administrators, for the mosque module when a platform administrator registers a mosque with
 * its first administrator. The caller checks that the actor is a platform administrator.
 */
public interface AccountProvisioning {

    /**
     * The active account with this email address, or a new account that receives an invitation email with a code
     * to choose a password. Runs in the caller's transaction: if the caller fails, no account and no email remain.
     *
     * @throws nl.mijnummah.common.error.ApplicationException {@code ACCOUNT_BLOCKED} for a blocked account,
     *                                                        {@code PASSWORD_RESET_UNAVAILABLE} when a new
     *                                                        account is needed but email is not configured
     */
    ProvisionedAccount findOrInvite(UUID actorId, String email, String displayName);

    /** @param invited true when the account was created just now */
    record ProvisionedAccount(UUID id, boolean invited) {
    }
}
