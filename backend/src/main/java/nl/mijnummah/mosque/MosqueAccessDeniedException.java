package nl.mijnummah.mosque;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/** The caller is signed in but has no (sufficient) rights for this mosque. */
public class MosqueAccessDeniedException extends ApplicationException {

    public MosqueAccessDeniedException() {
        super(ErrorCategory.FORBIDDEN, "MOSQUE_ACCESS_DENIED", "You do not have permission for this mosque.");
    }
}
