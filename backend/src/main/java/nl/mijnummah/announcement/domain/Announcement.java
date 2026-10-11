package nl.mijnummah.announcement.domain;

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

/** A news item of one mosque. */
@Entity
@Table(name = "announcement")
public class Announcement {

    @Id
    private UUID id;

    @Column(name = "mosque_id", nullable = false, updatable = false)
    private UUID mosqueId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "body", nullable = false, length = 5000)
    private String body;

    @Column(name = "featured", nullable = false)
    private boolean featured;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AnnouncementStatus status;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Announcement() {
        // for JPA
    }

    public static Announcement draft(UUID mosqueId, AnnouncementDetails details, UUID actorId, Instant now) {
        Announcement announcement = new Announcement();
        announcement.id = UUID.randomUUID();
        announcement.mosqueId = Objects.requireNonNull(mosqueId, "mosqueId");
        announcement.status = AnnouncementStatus.DRAFT;
        announcement.createdAt = Objects.requireNonNull(now, "now");
        announcement.createdBy = Objects.requireNonNull(actorId, "actorId");
        announcement.apply(details, actorId, now);
        return announcement;
    }

    public void update(AnnouncementDetails details, UUID actorId, Instant now) {
        apply(details, actorId, now);
    }

    /** Publishing an already expired announcement would show nothing, so it is refused. */
    public void publish(UUID actorId, Instant now) {
        if (isExpired(now)) {
            throw new InvalidAnnouncementException("The expiry date has passed. Change expiresAt before publishing.");
        }
        status = AnnouncementStatus.PUBLISHED;
        if (publishedAt == null) {
            publishedAt = now;
        }
        touch(actorId, now);
    }

    public void unpublish(UUID actorId, Instant now) {
        status = AnnouncementStatus.DRAFT;
        touch(actorId, now);
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }

    private void apply(AnnouncementDetails details, UUID actorId, Instant now) {
        title = details.title();
        body = details.body();
        featured = details.featured();
        expiresAt = details.expiresAt();
        touch(actorId, now);
    }

    private void touch(UUID actorId, Instant now) {
        updatedBy = Objects.requireNonNull(actorId, "actorId");
        updatedAt = Objects.requireNonNull(now, "now");
    }

    public AnnouncementDetails details() {
        return new AnnouncementDetails(title, body, featured, expiresAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getMosqueId() {
        return mosqueId;
    }

    public AnnouncementStatus getStatus() {
        return status;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
