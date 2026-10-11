package nl.mijnummah.identity.application;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/** The action is reserved for platform administrators. */
public class PlatformAdminRequiredException extends ApplicationException {

    public PlatformAdminRequiredException() {
        super(ErrorCategory.FORBIDDEN, "FORBIDDEN", "Only a platform administrator can perform this action.");
    }
}
