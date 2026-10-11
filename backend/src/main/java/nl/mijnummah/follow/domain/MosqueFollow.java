package nl.mijnummah.follow.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** A user follows a mosque. */
@Entity
@Table(name = "mosque_follow")
@IdClass(MosqueFollow.Key.class)
public class MosqueFollow {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Id
    @Column(name = "mosque_id", nullable = false, updatable = false)
    private UUID mosqueId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MosqueFollow() {
        // for JPA
    }

    public static MosqueFollow start(UUID userId, UUID mosqueId, Instant now) {
        MosqueFollow follow = new MosqueFollow();
        follow.userId = Objects.requireNonNull(userId, "userId");
        follow.mosqueId = Objects.requireNonNull(mosqueId, "mosqueId");
        follow.createdAt = Objects.requireNonNull(now, "now");
        return follow;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getMosqueId() {
        return mosqueId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public record Key(UUID userId, UUID mosqueId) implements Serializable {

        public Key() {
            this(null, null);
        }
    }
}
