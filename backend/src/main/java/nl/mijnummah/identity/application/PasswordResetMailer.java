package nl.mijnummah.identity.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import nl.mijnummah.common.mail.MailMessage;
import nl.mijnummah.common.mail.Mailer;

/**
 * Sends the reset code by email, after the code was committed and outside the request thread: the response then
 * takes equally long whether or not the address has an account, and a slow mail server does not hold up the app.
 */
@Component
class PasswordResetMailer {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailer.class);

    private final Mailer mailer;

    PasswordResetMailer(Mailer mailer) {
        this.mailer = mailer;
    }

    @Async
    @TransactionalEventListener
    void on(PasswordResetCodeIssued issued) {
        try {
            mailer.send(new MailMessage(issued.email(), "Je code om je wachtwoord te herstellen", text(issued)));
        } catch (RuntimeException ex) {
            // no address or code in the log; SMTP error messages often contain the address, so only the type
            log.warn("Password reset email could not be sent: {}", ex.getClass().getSimpleName());
        }
    }

    static String text(PasswordResetCodeIssued issued) {
        return """
                Salaam %s,

                Je hebt gevraagd om je wachtwoord te herstellen. Vul deze code in de app in:

                    %s

                De code is %d minuten geldig en werkt één keer.

                Heb je dit niet zelf gevraagd? Dan hoef je niets te doen; je wachtwoord blijft hetzelfde.
                """.formatted(issued.displayName(), PasswordResetService.format(issued.code()),
                PasswordResetService.CODE_TTL.toMinutes());
    }
}
