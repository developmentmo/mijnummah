package nl.mijnummah.follow.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import nl.mijnummah.follow.NotificationTopic;

/** A follower's choice for one topic of one mosque. A missing row means "enabled". */
@Entity
@Table(name = "notification_preference")
@IdClass(NotificationPreference.Key.class)
public class NotificationPreference {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Id
    @Column(name = "mosque_id", nullable = false, updatable = false)
    private UUID mosqueId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "topic", nullable = false, updatable = false, length = 30)
    private NotificationTopic topic;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationPreference() {
        // for JPA
    }

    public static NotificationPreference of(UUID userId, UUID mosqueId, NotificationTopic topic, boolean enabled,
                                            Instant now) {
        NotificationPreference preference = new NotificationPreference();
        preference.userId = Objects.requireNonNull(userId, "userId");
        preference.mosqueId = Objects.requireNonNull(mosqueId, "mosqueId");
        preference.topic = Objects.requireNonNull(topic, "topic");
        preference.change(enabled, now);
        return preference;
    }

    public void change(boolean enabled, Instant now) {
        this.enabled = enabled;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public NotificationTopic getTopic() {
        return topic;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public UUID getMosqueId() {
        return mosqueId;
    }

    public record Key(UUID userId, UUID mosqueId, NotificationTopic topic) implements Serializable {

        public Key() {
            this(null, null, null);
        }
    }
}
