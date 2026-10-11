package nl.mijnummah.media.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** A recording of one mosque, referencing a video at an external provider. */
@Entity
@Table(name = "media_item")
public class MediaItem {

    @Id
    private UUID id;

    @Column(name = "mosque_id", nullable = false, updatable = false)
    private UUID mosqueId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", length = 10000)
    private String description;

    @Column(name = "speaker", length = 100)
    private String speaker;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private MediaCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "video_provider", nullable = false, length = 20)
    private VideoProviderType videoProvider;

    @Column(name = "video_id", nullable = false, length = 100)
    private String videoId;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "recorded_on")
    private LocalDate recordedOn;

    @Column(name = "related_event_id")
    private UUID relatedEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private Visibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MediaStatus status;

    @Column(name = "published_at")
    private Instant publishedAt;

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

    protected MediaItem() {
        // for JPA
    }

    public static MediaItem draft(UUID mosqueId, MediaDetails details, UUID actorId, Instant now) {
        MediaItem item = new MediaItem();
        item.id = UUID.randomUUID();
        item.mosqueId = Objects.requireNonNull(mosqueId, "mosqueId");
        item.status = MediaStatus.DRAFT;
        item.createdAt = Objects.requireNonNull(now, "now");
        item.createdBy = Objects.requireNonNull(actorId, "actorId");
        item.apply(details, actorId, now);
        return item;
    }

    public void update(MediaDetails details, UUID actorId, Instant now) {
        apply(details, actorId, now);
    }

    /** The first publication time is kept, so re-publishing does not move an item to the top of the list. */
    public void publish(UUID actorId, Instant now) {
        status = MediaStatus.PUBLISHED;
        if (publishedAt == null) {
            publishedAt = now;
        }
        touch(actorId, now);
    }

    public void unpublish(UUID actorId, Instant now) {
        status = MediaStatus.DRAFT;
        touch(actorId, now);
    }

    private void apply(MediaDetails details, UUID actorId, Instant now) {
        title = details.title();
        description = details.description();
        speaker = details.speaker();
        category = details.category();
        videoProvider = details.video().provider();
        videoId = details.video().videoId();
        thumbnailUrl = details.thumbnailUrl();
        durationSeconds = details.durationSeconds();
        recordedOn = details.recordedOn();
        relatedEventId = details.relatedEventId();
        visibility = details.visibility();
        touch(actorId, now);
    }

    private void touch(UUID actorId, Instant now) {
        updatedBy = Objects.requireNonNull(actorId, "actorId");
        updatedAt = Objects.requireNonNull(now, "now");
    }

    public MediaDetails details() {
        return new MediaDetails(title, description, speaker, category, new VideoReference(videoProvider, videoId),
                thumbnailUrl, durationSeconds, recordedOn, relatedEventId, visibility);
    }

    public UUID getId() {
        return id;
    }

    public UUID getMosqueId() {
        return mosqueId;
    }

    public MediaCategory getCategory() {
        return category;
    }

    public String getSpeaker() {
        return speaker;
    }

    public UUID getRelatedEventId() {
        return relatedEventId;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public MediaStatus getStatus() {
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
