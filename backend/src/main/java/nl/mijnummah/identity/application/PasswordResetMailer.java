package nl.mijnummah.identity.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import nl.mijnummah.common.mail.MailMessage;
import nl.mijnummah.common.mail.MailProperties;
import nl.mijnummah.common.mail.Mailer;

/**
 * Sends the reset code by email, after the code was committed and outside the request thread: the response then
 * takes equally long whether or not the address has an account, and a slow mail server does not hold up the app.
 */
@Component
class PasswordResetMailer {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailer.class);

    private final Mailer mailer;
    private final String portalUrl;

    PasswordResetMailer(Mailer mailer, MailProperties properties) {
        this.mailer = mailer;
        this.portalUrl = properties.portalUrl();
    }

    @Async
    @TransactionalEventListener
    void on(PasswordResetCodeIssued issued) {
        try {
            mailer.send(new MailMessage(issued.email(), subject(issued), text(issued, portalUrl)));
        } catch (RuntimeException ex) {
            // no address or code in the log; SMTP error messages often contain the address, so only the type
            log.warn("Password reset email could not be sent: {}", ex.getClass().getSimpleName());
        }
    }

    static String subject(PasswordResetCodeIssued issued) {
        return switch (issued.kind()) {
            case SELF_SERVICE -> "Je code om je wachtwoord te herstellen";
            case INVITATION -> "Je account bij Mijn Ummah";
            case BY_ADMINISTRATOR -> "Je code om een nieuw wachtwoord in te stellen";
        };
    }

    /** {@code portalUrl}: empty for no link. */
    static String text(PasswordResetCodeIssued issued, String portalUrl) {
        String code = PasswordResetService.format(issued.code());
        String where = portalUrl.isEmpty()
                ? "in de app (Wachtwoord vergeten) of in het beheerportaal (Wachtwoord instellen)"
                : "in de app (Wachtwoord vergeten) of op " + portalUrl + "/wachtwoord-instellen";
        return switch (issued.kind()) {
            case SELF_SERVICE -> """
                    Salaam %s,

                    Je hebt gevraagd om je wachtwoord te herstellen. Vul deze code in de app in:

                        %s

                    De code is %d minuten geldig en werkt één keer.

                    Heb je dit niet zelf gevraagd? Dan hoef je niets te doen; je wachtwoord blijft hetzelfde.
                    """.formatted(issued.displayName(), code, PasswordResetService.CODE_TTL.toMinutes());
            case INVITATION -> """
                    Salaam %s,

                    Er is een account voor je aangemaakt bij Mijn Ummah. Kies je eigen wachtwoord met deze code,
                    %s:

                        %s

                    De code is %d uur geldig en werkt één keer. Daarna log je in met dit e-mailadres.

                    Verwachtte je deze e-mail niet? Dan hoef je niets te doen.
                    """.formatted(issued.displayName(), where, code,
                    PasswordResetService.ADMINISTRATOR_CODE_TTL.toHours());
            case BY_ADMINISTRATOR -> """
                    Salaam %s,

                    De beheerder van Mijn Ummah heeft voor je een code aangevraagd om een nieuw wachtwoord in te
                    stellen. Vul de code in %s:

                        %s

                    De code is %d uur geldig en werkt één keer.

                    Heb je hier niet om gevraagd? Dan hoef je niets te doen; je wachtwoord blijft hetzelfde.
                    """.formatted(issued.displayName(), where, code,
                    PasswordResetService.ADMINISTRATOR_CODE_TTL.toHours());
        };
    }
}
