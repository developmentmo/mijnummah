package nl.mijnummah.common.mail;

/** The mail server did not accept the message. Not shown to API clients: sending happens in the background. */
public class MailDeliveryException extends RuntimeException {

    public MailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
