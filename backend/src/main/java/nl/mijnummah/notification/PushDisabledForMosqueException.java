package nl.mijnummah.notification;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/** The platform administrator switched push notifications off for this mosque (mosque settings). */
public class PushDisabledForMosqueException extends ApplicationException {

    public PushDisabledForMosqueException() {
        super(ErrorCategory.BUSINESS_RULE_VIOLATION, "PUSH_DISABLED_FOR_MOSQUE",
                "Push notifications are switched off for this mosque.");
    }
}
