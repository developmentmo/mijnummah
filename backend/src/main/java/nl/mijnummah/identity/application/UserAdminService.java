package nl.mijnummah.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.common.audit.AuditEvent;
import nl.mijnummah.common.crypto.OpaqueTokens;
import nl.mijnummah.identity.AccountProvisioning;
import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.identity.PlatformRole;
import nl.mijnummah.identity.domain.PasswordPolicy;
import nl.mijnummah.identity.domain.User;
import nl.mijnummah.identity.domain.UserStatus;
import nl.mijnummah.identity.infrastructure.RefreshTokenRepository;
import nl.mijnummah.identity.infrastructure.UserRepository;

/**
 * Account management by platform administrators: creating accounts (e.g. for a mosque's administrators, who are
 * then given rights per mosque by the mosque module), listing, blocking and unblocking, and sending a code to set a
 * new password. An account created without a password gets an invitation email to choose one.
 */
@Service
public class UserAdminService implements AccountProvisioning {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final ResetCodeIssuer resetCodes;
    private final ApplicationEventPublisher audit;
    private final Clock clock;

    public UserAdminService(UserRepository users, RefreshTokenRepository refreshTokens,
                            PasswordEncoder passwordEncoder, ResetCodeIssuer resetCodes,
                            ApplicationEventPublisher audit, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.resetCodes = resetCodes;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * Without a password ({@code null}), the account gets an invitation email with a code to choose one; that needs
     * email to be configured.
     */
    @Transactional
    public UserProfile create(AuthenticatedUser actor, String email, String displayName, String password,
                              PlatformRole role) {
        requirePlatformAdmin(actor);
        if (password == null) {
            return invite(actor.id(), email, displayName, role);
        }
        UserProfile created = createUnchecked(email, displayName, password, role);
        audit.publishEvent(AuditEvent.of(actor.id(), null, "ACCOUNT_CREATED", "ACCOUNT", created.id()));
        return created;
    }

    @Override
    @Transactional
    public ProvisionedAccount findOrInvite(UUID actorId, String email, String displayName) {
        var existing = users.findByEmail(User.normalizeEmail(email));
        if (existing.isPresent()) {
            if (!existing.get().isActive()) {
                throw new AccountBlockedException();
            }
            return new ProvisionedAccount(existing.get().getId(), false);
        }
        return new ProvisionedAccount(invite(actorId, email, displayName, PlatformRole.USER).id(), true);
    }

    /** Sends the account a code to set a new password, e.g. when the user cannot reset it themselves. */
    @Transactional
    public void sendPasswordCode(AuthenticatedUser actor, UUID userId) {
        requirePlatformAdmin(actor);
        User user = users.findById(userId).orElseThrow(UserNotFoundException::new);
        if (!user.isActive()) {
            throw new AccountBlockedException();
        }
        resetCodes.requireMail();
        resetCodes.issue(user, PasswordResetCodeIssued.Kind.BY_ADMINISTRATOR, PasswordResetService.ADMINISTRATOR_CODE_TTL,
                Instant.now(clock));
        audit.publishEvent(AuditEvent.of(actor.id(), null, "ACCOUNT_PASSWORD_CODE_SENT", "ACCOUNT", userId));
    }

    /** A new account with an unknown random password; the invitation code lets the user choose their own. */
    private UserProfile invite(UUID actorId, String email, String displayName, PlatformRole role) {
        resetCodes.requireMail();
        String normalizedEmail = User.normalizeEmail(email);
        if (users.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }
        User user = User.create(normalizedEmail, passwordEncoder.encode(OpaqueTokens.generate()), displayName, role,
                Instant.now(clock));
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyRegisteredException();
        }
        resetCodes.issue(user, PasswordResetCodeIssued.Kind.INVITATION, PasswordResetService.ADMINISTRATOR_CODE_TTL,
                Instant.now(clock));
        audit.publishEvent(AuditEvent.of(actorId, null, "ACCOUNT_INVITED", "ACCOUNT", user.getId()));
        return UserProfile.of(user);
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
    /** {@code query}: part of the email address or name, case-insensitive; null for all. */
    public Page<UserProfile> list(AuthenticatedUser actor, String query, UserStatus status, Pageable pageable) {
        requirePlatformAdmin(actor);
        String pattern = query == null || query.isBlank() ? null
                : "%" + query.strip().toLowerCase(java.util.Locale.ROOT).replace("\\", "\\\\")
                        .replace("%", "\\%").replace("_", "\\_") + "%";
        return users.search(pattern, status, pageable).map(UserProfile::of);
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
        audit.publishEvent(AuditEvent.of(actor.id(), null, "ACCOUNT_BLOCKED", "ACCOUNT", userId));
        return UserProfile.of(user);
    }

    @Transactional
    public UserProfile unblock(AuthenticatedUser actor, UUID userId) {
        requirePlatformAdmin(actor);
        User user = users.findById(userId).orElseThrow(UserNotFoundException::new);
        user.unblock(Instant.now(clock));
        audit.publishEvent(AuditEvent.of(actor.id(), null, "ACCOUNT_UNBLOCKED", "ACCOUNT", userId));
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
