package nl.mijnummah.notification;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/** No push service is configured for this environment. */
public class PushNotConfiguredException extends ApplicationException {

    public PushNotConfiguredException() {
        super(ErrorCategory.UNAVAILABLE, "PUSH_NOT_CONFIGURED", "Push notifications are not set up yet.");
    }
}
