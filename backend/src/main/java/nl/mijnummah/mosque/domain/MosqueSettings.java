package nl.mijnummah.mosque.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Configuration of one mosque, created with it and changed only by a platform administrator. */
@Entity
@Table(name = "mosque_settings")
public class MosqueSettings {

    @Id
    @Column(name = "mosque_id")
    private UUID mosqueId;

    @Column(name = "registrations_enabled", nullable = false)
    private boolean registrationsEnabled;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MosqueSettings() {
        // for JPA
    }

    public static MosqueSettings create(UUID mosqueId, boolean registrationsEnabled, boolean pushEnabled,
                                        Instant now) {
        MosqueSettings settings = new MosqueSettings();
        settings.mosqueId = Objects.requireNonNull(mosqueId, "mosqueId");
        settings.change(registrationsEnabled, pushEnabled, now);
        return settings;
    }

    public void change(boolean registrationsEnabled, boolean pushEnabled, Instant now) {
        this.registrationsEnabled = registrationsEnabled;
        this.pushEnabled = pushEnabled;
        this.updatedAt = now;
    }

    public UUID getMosqueId() {
        return mosqueId;
    }

    public boolean isRegistrationsEnabled() {
        return registrationsEnabled;
    }

    public boolean isPushEnabled() {
        return pushEnabled;
    }
}
