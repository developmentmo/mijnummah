package nl.mijnummah.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.identity.PlatformRole;
import nl.mijnummah.identity.domain.PasswordPolicy;
import nl.mijnummah.identity.domain.User;
import nl.mijnummah.identity.infrastructure.RefreshTokenRepository;
import nl.mijnummah.identity.infrastructure.UserRepository;

/**
 * Account management by platform administrators: creating accounts (e.g. for a mosque's administrators, who are
 * then given rights per mosque by the mosque module), listing, blocking and unblocking.
 */
@Service
public class UserAdminService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserAdminService(UserRepository users, RefreshTokenRepository refreshTokens,
                            PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public UserProfile create(AuthenticatedUser actor, String email, String displayName, String password,
                              PlatformRole role) {
        requirePlatformAdmin(actor);
        return createUnchecked(email, displayName, password, role);
    }

    /** Without an actor: only for the startup bootstrap of the first platform administrator. */
    @Transactional
    public UserProfile createUnchecked(String email, String displayName, String password, PlatformRole role) {
        PasswordPolicy.validate(password);
        String normalizedEmail = User.normalizeEmail(email);
        if (users.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }
        User user = User.create(normalizedEmail, passwordEncoder.encode(password), displayName, role,
                Instant.now(clock));
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // concurrent creation with the same email hit the unique constraint
            throw new EmailAlreadyRegisteredException();
        }
        return UserProfile.of(user);
    }

    @Transactional(readOnly = true)
    public Page<UserProfile> list(AuthenticatedUser actor, Pageable pageable) {
        requirePlatformAdmin(actor);
        return users.findAll(pageable).map(UserProfile::of);
    }

    @Transactional(readOnly = true)
    public UserProfile profile(UUID userId) {
        return users.findById(userId).map(UserProfile::of).orElseThrow(UserNotFoundException::new);
    }

    /** Blocks the account and ends all its sessions; the access token stops working at the next request. */
    @Transactional
    public UserProfile block(AuthenticatedUser actor, UUID userId) {
        requirePlatformAdmin(actor);
        if (actor.id().equals(userId)) {
            throw new CannotChangeOwnAccountException();
        }
        User user = users.findById(userId).orElseThrow(UserNotFoundException::new);
        Instant now = Instant.now(clock);
        user.block(now);
        refreshTokens.revokeAllOfUser(userId, now);
        return UserProfile.of(user);
    }

    @Transactional
    public UserProfile unblock(AuthenticatedUser actor, UUID userId) {
        requirePlatformAdmin(actor);
        User user = users.findById(userId).orElseThrow(UserNotFoundException::new);
        user.unblock(Instant.now(clock));
        return UserProfile.of(user);
    }

    @Transactional(readOnly = true)
    public boolean platformAdminExists() {
        return users.existsByPlatformRole(PlatformRole.PLATFORM_ADMIN);
    }

    private static void requirePlatformAdmin(AuthenticatedUser actor) {
        if (!actor.isPlatformAdmin()) {
            throw new PlatformAdminRequiredException();
        }
    }
}
