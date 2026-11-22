package nl.mijnummah.identity.application;

import java.time.Duration;
import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.common.mail.Mailer;
import nl.mijnummah.identity.domain.PasswordResetCode;
import nl.mijnummah.identity.domain.User;
import nl.mijnummah.identity.infrastructure.PasswordResetCodeRepository;

/**
 * Stores a new one-time code for setting a password (replacing open codes) and publishes it for the email, which
 * goes out after commit. Shared by the user's own reset request and by codes a platform administrator sends.
 */
@Component
class ResetCodeIssuer {

    private final PasswordResetCodeRepository codes;
    private final Mailer mailer;
    private final ApplicationEventPublisher events;

    ResetCodeIssuer(PasswordResetCodeRepository codes, Mailer mailer, ApplicationEventPublisher events) {
        this.codes = codes;
        this.mailer = mailer;
        this.events = events;
    }

    /** @throws PasswordResetUnavailableException when email is not configured */
    void requireMail() {
        if (!mailer.available()) {
            throw new PasswordResetUnavailableException();
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void issue(User user, PasswordResetCodeIssued.Kind kind, Duration ttl, Instant now) {
        codes.invalidateOpenCodes(user.getId(), now);
        String code = PasswordResetService.generate();
        codes.save(PasswordResetCode.issue(user.getId(), PasswordResetService.hash(user.getId(), code), now, ttl));
        events.publishEvent(new PasswordResetCodeIssued(user.getEmail(), user.getDisplayName(), code, kind));
    }
}
