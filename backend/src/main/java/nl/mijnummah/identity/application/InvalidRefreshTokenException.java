package nl.mijnummah.identity.application;

import nl.mijnummah.common.error.ApplicationException;
import nl.mijnummah.common.error.ErrorCategory;

/** Unknown, expired, revoked or replayed refresh token. Deliberately one answer for all cases. */
public class InvalidRefreshTokenException extends ApplicationException {

    public InvalidRefreshTokenException() {
        super(ErrorCategory.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired.");
    }
}
