package nl.mijnummah.common.mail;

/**
 * Sends emails. Which implementation is active is decided by {@code mijnummah.mail.provider}:
 * {@code none} (default: email is not available), {@code smtp} (a real SMTP server, {@code spring.mail.*}) or
 * {@code log} (local development only: the message is written to the log instead of being sent).
 */
public interface Mailer {

    /** Whether emails can actually be delivered (or, for {@code log}, shown to the developer). */
    boolean available();

    /** @throws MailDeliveryException when the message could not be handed over */
    void send(MailMessage message);
}
