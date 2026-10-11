package nl.mijnummah.mosque.application;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/** Slugs are unique across the platform. */
public class SlugAlreadyTakenException extends ApplicationException {

    public SlugAlreadyTakenException() {
        super(ErrorCategory.CONFLICT, "SLUG_ALREADY_TAKEN", "Another mosque already uses this slug.");
    }
}
