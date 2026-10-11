package nl.mijnummah.notification;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/** Only content that is visible in the app can be announced with a push notification. */
public class PushContentNotPublishedException extends ApplicationException {

    public PushContentNotPublishedException(String message) {
        super(ErrorCategory.BUSINESS_RULE_VIOLATION, "CONTENT_NOT_PUBLISHED", message);
    }
}
