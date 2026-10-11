package nl.mijnummah.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.common.crypto.OpaqueTokens;
import nl.mijnummah.identity.domain.RefreshToken;
import nl.mijnummah.identity.domain.User;
import nl.mijnummah.identity.domain.UserStatus;
import nl.mijnummah.identity.infrastructure.RefreshTokenRepository;
import nl.mijnummah.identity.infrastructure.SecurityProperties;
import nl.mijnummah.identity.infrastructure.UserRepository;

/**
 * Login, refresh-token rotation and logout. Self-registration of app users follows in fase 5; until then
 * accounts are created by a platform administrator ({@link UserAdminService}).
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;
    private final SecurityProperties properties;
    private final Clock clock;
    private final LoginThrottle loginThrottle;

    /**
     * Hash compared against when the email is unknown, so login takes about as long for unknown emails as for
     * wrong passwords (prevents account enumeration by timing).
     */
    private final String dummyPasswordHash;

    public AuthService(UserRepository users,
                       RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder,
                       AccessTokenService accessTokenService,
                       SecurityProperties properties,
                       Clock clock,
                       LoginThrottle loginThrottle) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.properties = properties;
        this.clock = clock;
        this.loginThrottle = loginThrottle;
        this.dummyPasswordHash = passwordEncoder.encode(OpaqueTokens.generate());
    }

    @Transactional
    public AuthTokens login(String email, String password) {
        String normalizedEmail = User.normalizeEmail(email);
        loginThrottle.checkNotBlocked(normalizedEmail);
        Optional<User> candidate = users.findByEmail(normalizedEmail);
        if (candidate.isEmpty()) {
            passwordEncoder.matches(password, dummyPasswordHash);
            loginThrottle.recordFailure(normalizedEmail);
            throw new InvalidCredentialsException();
        }
        User user = candidate.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            loginThrottle.recordFailure(normalizedEmail);
            throw new InvalidCredentialsException();
        }
        // Only after a correct password, so the answer does not reveal blocked accounts to others.
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AccountBlockedException();
        }
        loginThrottle.reset(normalizedEmail);
        return issueTokens(user.getId(), UUID.randomUUID(), Instant.now(clock));
    }

    /**
     * Rotates a refresh token. {@code noRollbackFor}: when reuse is detected, the family revocation must be
     * committed even though the request fails.
     */
    @Transactional(noRollbackFor = {InvalidRefreshTokenException.class, AccountBlockedException.class})
    public AuthTokens refresh(String rawRefreshToken) {
        Instant now = Instant.now(clock);
        RefreshToken current = refreshTokens.findByTokenHash(OpaqueTokens.sha256Hex(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.isRevoked()) {
            // Replay of a rotated or logged-out token: assume theft, end the whole login session.
            refreshTokens.revokeFamily(current.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (current.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        User user = users.findById(current.getUserId()).orElseThrow(InvalidRefreshTokenException::new);
        if (!user.isActive()) {
            refreshTokens.revokeFamily(current.getFamilyId(), now);
            throw new AccountBlockedException();
        }

        String rawSuccessor = OpaqueTokens.generate();
        RefreshToken successor = RefreshToken.issue(user.getId(), current.getFamilyId(),
                OpaqueTokens.sha256Hex(rawSuccessor), now, properties.refreshTokenTtl());
        refreshTokens.save(successor);
        current.rotateTo(successor, now);

        AccessTokenService.IssuedAccessToken accessToken = accessTokenService.issue(user.getId());
        return new AuthTokens(accessToken.value(), accessToken.expiresAt(), rawSuccessor, successor.getExpiresAt());
    }

    /** Ends the login session the token belongs to. Idempotent; unknown tokens are ignored. */
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.findByTokenHash(OpaqueTokens.sha256Hex(rawRefreshToken))
                .ifPresent(token -> refreshTokens.revokeFamily(token.getFamilyId(), Instant.now(clock)));
    }

    private AuthTokens issueTokens(UUID userId, UUID familyId, Instant now) {
        String rawRefreshToken = OpaqueTokens.generate();
        RefreshToken refreshToken = RefreshToken.issue(userId, familyId,
                OpaqueTokens.sha256Hex(rawRefreshToken), now, properties.refreshTokenTtl());
        refreshTokens.save(refreshToken);
        AccessTokenService.IssuedAccessToken accessToken = accessTokenService.issue(userId);
        return new AuthTokens(accessToken.value(), accessToken.expiresAt(), rawRefreshToken,
                refreshToken.getExpiresAt());
    }
}
