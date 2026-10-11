package nl.mijnummah.identity.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import nl.mijnummah.identity.PlatformRole;

/** A person with an account. */
@Entity
@Table(name = "app_user")
public class User {

    @Id
    private UUID id;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform_role", nullable = false, length = 20)
    private PlatformRole platformRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic locking; a null version also tells Spring Data the entity is new. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected User() {
        // for JPA
    }

    /** Creates an active account. The role is decided by the caller (a platform admin or the bootstrap). */
    public static User create(String email, String passwordHash, String displayName, PlatformRole role, Instant now) {
        User user = new User();
        user.id = UUID.randomUUID();
        user.email = normalizeEmail(email);
        user.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        user.displayName = requireText(displayName, "displayName");
        user.platformRole = Objects.requireNonNull(role, "role");
        user.status = UserStatus.ACTIVE;
        user.createdAt = Objects.requireNonNull(now, "now");
        user.updatedAt = now;
        return user;
    }

    /** Canonical form used for storage and lookup: trimmed, lower-case (locale-independent). */
    public static String normalizeEmail(String email) {
        return requireText(email, "email").toLowerCase(Locale.ROOT);
    }

    public void block(Instant now) {
        status = UserStatus.BLOCKED;
        updatedAt = now;
    }

    public void unblock(Instant now) {
        status = UserStatus.ACTIVE;
        updatedAt = now;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public PlatformRole getPlatformRole() {
        return platformRole;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
