package nl.mijnummah.announcement.domain;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

public class InvalidAnnouncementException extends ApplicationException {

    public InvalidAnnouncementException(String message) {
        super(ErrorCategory.INVALID_REQUEST, "INVALID_ANNOUNCEMENT", message);
    }
}
