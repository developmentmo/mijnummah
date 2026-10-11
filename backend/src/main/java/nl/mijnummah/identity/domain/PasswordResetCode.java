package nl.mijnummah.identity.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A one-time code for resetting a forgotten password. Only a hash of the code is stored.
 * <p>
 * A code is short enough to type over from an email, so it is protected by a short lifetime, a small number of
 * attempts (after which it is invalidated) and per-IP rate limits, not by its length alone.
 */
@Entity
@Table(name = "password_reset_code")
public class PasswordResetCode {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "code_hash", nullable = false, updatable = false, length = 64)
    private String codeHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected PasswordResetCode() {
        // for JPA
    }

    public static PasswordResetCode issue(UUID userId, String codeHash, Instant now, Duration ttl) {
        PasswordResetCode code = new PasswordResetCode();
        code.id = UUID.randomUUID();
        code.userId = Objects.requireNonNull(userId, "userId");
        code.codeHash = Objects.requireNonNull(codeHash, "codeHash");
        code.createdAt = Objects.requireNonNull(now, "now");
        code.expiresAt = now.plus(ttl);
        return code;
    }

    /** Unused, not invalidated and not expired. */
    public boolean isUsable(Instant now) {
        return usedAt == null && invalidatedAt == null && now.isBefore(expiresAt);
    }

    /** Constant-time comparison of hashes. */
    public boolean matches(String candidateHash) {
        return MessageDigest.isEqual(codeHash.getBytes(StandardCharsets.US_ASCII),
                candidateHash.getBytes(StandardCharsets.US_ASCII));
    }

    /** Counts a wrong code; at {@code maxAttempts} the code stops working. */
    public void recordFailedAttempt(int maxAttempts, Instant now) {
        attempts++;
        if (attempts >= maxAttempts) {
            invalidate(now);
        }
    }

    public void markUsed(Instant now) {
        usedAt = now;
    }

    public void invalidate(Instant now) {
        if (invalidatedAt == null) {
            invalidatedAt = now;
        }
    }

    public UUID getUserId() {
        return userId;
    }

    public int getAttempts() {
        return attempts;
    }
}
