package nl.mijnummah.mosque.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import nl.mijnummah.mosque.MosqueRole;

/** A user's rights for one mosque. */
@Entity
@Table(name = "mosque_administrator")
public class MosqueAdministrator {

    @Id
    private UUID id;

    @Column(name = "mosque_id", nullable = false, updatable = false)
    private UUID mosqueId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MosqueRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    protected MosqueAdministrator() {
        // for JPA
    }

    public static MosqueAdministrator grant(UUID mosqueId, UUID userId, MosqueRole role, UUID createdBy, Instant now) {
        MosqueAdministrator administrator = new MosqueAdministrator();
        administrator.id = UUID.randomUUID();
        administrator.mosqueId = Objects.requireNonNull(mosqueId, "mosqueId");
        administrator.userId = Objects.requireNonNull(userId, "userId");
        administrator.role = Objects.requireNonNull(role, "role");
        administrator.createdBy = createdBy;
        administrator.createdAt = Objects.requireNonNull(now, "now");
        return administrator;
    }

    public void changeRole(MosqueRole role) {
        this.role = Objects.requireNonNull(role, "role");
    }

    public UUID getId() {
        return id;
    }

    public UUID getMosqueId() {
        return mosqueId;
    }

    public UUID getUserId() {
        return userId;
    }

    public MosqueRole getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
