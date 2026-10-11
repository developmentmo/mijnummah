package nl.mijnummah.notification.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * The push token of one app installation, owned by the user who is signed in on it. Written only through
 * {@code PushDeviceRepository#upsert}.
 */
@Entity
@Table(name = "push_device")
public class PushDevice {

    public static final int MAX_TOKEN_LENGTH = 512;

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token", nullable = false, updatable = false, length = MAX_TOKEN_LENGTH)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private DevicePlatform platform;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected PushDevice() {
        // for JPA
    }

    public UUID getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public DevicePlatform getPlatform() {
        return platform;
    }
}
