package nl.mijnummah.identity.application;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.common.crypto.OpaqueTokens;
import nl.mijnummah.common.mail.Mailer;
import nl.mijnummah.identity.domain.PasswordPolicy;
import nl.mijnummah.identity.domain.PasswordResetCode;
import nl.mijnummah.identity.domain.User;
import nl.mijnummah.identity.infrastructure.PasswordResetCodeRepository;
import nl.mijnummah.identity.infrastructure.RefreshTokenRepository;
import nl.mijnummah.identity.infrastructure.UserRepository;

/**
 * Forgotten password: a one-time code by email, typed into the app together with a new password.
 * <p>
 * Requesting a code always gives the same answer, whether or not the address has an account. Confirming signs the
 * user in and ends all their other sessions.
 */
@Service
public class PasswordResetService {

    static final Duration CODE_TTL = Duration.ofMinutes(30);
    static final int MAX_ATTEMPTS = 5;
    static final int CODE_LENGTH = 8;

    /** No 0/O, 1/I/L: easy to read from an email and to type. 31^8 is about 8.5 * 10^11 codes. */
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final PasswordResetCodeRepository codes;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final Mailer mailer;
    private final PasswordResetThrottle throttle;
    private final LoginThrottle loginThrottle;
    private final AuthService authService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public PasswordResetService(UserRepository users, PasswordResetCodeRepository codes,
                                RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder, Mailer mailer,
                                PasswordResetThrottle throttle, LoginThrottle loginThrottle, AuthService authService,
                                ApplicationEventPublisher events, Clock clock) {
        this.users = users;
        this.codes = codes;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.mailer = mailer;
        this.throttle = throttle;
        this.loginThrottle = loginThrottle;
        this.authService = authService;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Sends a new code to an active account with this address, replacing earlier codes. Unknown and blocked
     * addresses, and addresses over their email limit, get the same answer without an email.
     *
     * @throws PasswordResetUnavailableException when email is not configured (for every address alike)
     */
    @Transactional
    public void request(String email) {
        if (!mailer.available()) {
            throw new PasswordResetUnavailableException();
        }
        String normalizedEmail = User.normalizeEmail(email);
        if (!throttle.tryAcquire(normalizedEmail)) {
            return;
        }
        users.findByEmail(normalizedEmail).filter(User::isActive).ifPresent(user -> {
            Instant now = Instant.now(clock);
            codes.invalidateOpenCodes(user.getId(), now);
            String code = generate();
            codes.save(PasswordResetCode.issue(user.getId(), hash(user.getId(), code), now, CODE_TTL));
            events.publishEvent(new PasswordResetCodeIssued(user.getEmail(), user.getDisplayName(), code));
        });
    }

    /**
     * Sets the new password when the code is right, signs the user in and ends all earlier sessions.
     * {@code noRollbackFor}: a wrong attempt must be counted even though the request fails.
     *
     * @throws InvalidResetCodeException for a wrong, expired or used code and for unknown addresses alike
     */
    @Transactional(noRollbackFor = InvalidResetCodeException.class)
    public AuthTokens confirm(String email, String code, String newPassword) {
        // First, so a weak password does not use up an attempt.
        PasswordPolicy.validate(newPassword);
        Instant now = Instant.now(clock);
        User user = users.findByEmail(User.normalizeEmail(email)).orElseThrow(InvalidResetCodeException::new);
        PasswordResetCode stored = codes
                .findFirstByUserIdAndUsedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(user.getId())
                .filter(candidate -> candidate.isUsable(now))
                .orElseThrow(InvalidResetCodeException::new);
        if (!stored.matches(hash(user.getId(), normalize(code)))) {
            stored.recordFailedAttempt(MAX_ATTEMPTS, now);
            throw new InvalidResetCodeException();
        }
        if (!user.isActive()) {
            throw new AccountBlockedException();
        }
        stored.markUsed(now);
        user.changePassword(passwordEncoder.encode(newPassword), now);
        refreshTokens.revokeAllOfUser(user.getId(), now);
        loginThrottle.reset(user.getEmail());
        return authService.issueFor(user.getId());
    }

    /** Upper case without spaces or dashes, so "abcd-efgh" and "ABCD EFGH" both work. */
    static String normalize(String code) {
        return code == null ? "" : code.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    /** As shown in the email: "ABCD-EFGH". */
    static String format(String code) {
        return code.substring(0, CODE_LENGTH / 2) + "-" + code.substring(CODE_LENGTH / 2);
    }

    static String generate() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }

    /** Bound to the user, so equal codes of different users have different hashes. */
    private static String hash(UUID userId, String code) {
        return OpaqueTokens.sha256Hex(userId + ":" + code);
    }
}
