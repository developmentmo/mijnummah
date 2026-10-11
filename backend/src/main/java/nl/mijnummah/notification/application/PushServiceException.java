package nl.mijnummah.notification.application;

/** The push service cannot be used at all, e.g. because the credentials were rejected. */
public class PushServiceException extends RuntimeException {

    public PushServiceException(String message) {
        super(message);
    }

    public PushServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
