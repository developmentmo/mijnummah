package nl.mijnummah.identity.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.identity.domain.PasswordPolicy;
import nl.mijnummah.identity.domain.User;
import nl.mijnummah.identity.infrastructure.RefreshTokenRepository;
import nl.mijnummah.identity.infrastructure.UserRepository;

/** What a signed-in user can do with their own account: change the password and delete the account. */
@Service
public class AccountService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final Clock clock;

    public AccountService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder,
                          AuthService authService, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.clock = clock;
    }

    /** Sets a new password, ends every session (also on other devices) and returns tokens for a new one. */
    @Transactional
    public AuthTokens changePassword(AuthenticatedUser actor, String currentPassword, String newPassword) {
        User user = requireOwn(actor, currentPassword);
        PasswordPolicy.validate(newPassword);
        Instant now = Instant.now(clock);
        user.changePassword(passwordEncoder.encode(newPassword), now);
        refreshTokens.revokeAllOfUser(user.getId(), now);
        return authService.issueFor(user.getId());
    }

    /**
     * Permanently deletes the account. The database removes everything personal with it: sessions, follows and
     * notification preferences, push devices, reset codes and event registrations.
     *
     * @throws AccountDeletionBlockedException when the account administers a mosque or authored content
     */
    @Transactional
    public void delete(AuthenticatedUser actor, String password) {
        User user = requireOwn(actor, password);
        try {
            users.delete(user);
            users.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new AccountDeletionBlockedException();
        }
    }

    private User requireOwn(AuthenticatedUser actor, String password) {
        User user = users.findById(actor.id()).orElseThrow(UserNotFoundException::new);
        if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new CurrentPasswordIncorrectException();
        }
        return user;
    }
}
