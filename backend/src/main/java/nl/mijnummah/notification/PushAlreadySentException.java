package nl.mijnummah.notification;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/** Followers are notified at most once per content item. */
public class PushAlreadySentException extends ApplicationException {

    public PushAlreadySentException() {
        super(ErrorCategory.CONFLICT, "PUSH_ALREADY_SENT", "A push notification was already sent for this item.");
    }
}
