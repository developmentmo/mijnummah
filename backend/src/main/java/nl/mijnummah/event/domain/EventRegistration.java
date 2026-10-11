package nl.mijnummah.event.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** One user's registration for one event. Cancelling keeps the row; registering again re-confirms it. */
@Entity
@Table(name = "event_registration")
public class EventRegistration {

    @Id
    private UUID id;

    @Column(name = "mosque_id", nullable = false, updatable = false)
    private UUID mosqueId;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RegistrationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected EventRegistration() {
        // for JPA
    }

    public static EventRegistration confirm(Event event, UUID userId, Instant now) {
        EventRegistration registration = new EventRegistration();
        registration.id = UUID.randomUUID();
        registration.mosqueId = event.getMosqueId();
        registration.eventId = event.getId();
        registration.userId = Objects.requireNonNull(userId, "userId");
        registration.status = RegistrationStatus.CONFIRMED;
        registration.createdAt = Objects.requireNonNull(now, "now");
        registration.updatedAt = now;
        return registration;
    }

    public void reconfirm(Instant now) {
        status = RegistrationStatus.CONFIRMED;
        updatedAt = now;
    }

    public void cancel(Instant now) {
        status = RegistrationStatus.CANCELLED;
        updatedAt = now;
    }

    public boolean isConfirmed() {
        return status == RegistrationStatus.CONFIRMED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMosqueId() {
        return mosqueId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getUserId() {
        return userId;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
