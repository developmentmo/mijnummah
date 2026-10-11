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

/** An agenda item of one mosque. Status changes go through the methods below, which enforce the lifecycle. */
@Entity
@Table(name = "event")
public class Event {

    @Id
    private UUID id;

    @Column(name = "mosque_id", nullable = false, updatable = false)
    private UUID mosqueId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", length = 10000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private EventCategory category;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private Visibility visibility;

    @Column(name = "registration_enabled", nullable = false)
    private boolean registrationEnabled;

    @Column(name = "capacity")
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventStatus status;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

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

    protected Event() {
        // for JPA
    }

    /** New events start as DRAFT: nothing becomes public without an explicit publish. */
    public static Event draft(UUID mosqueId, EventDetails details, UUID actorId, Instant now) {
        Event event = new Event();
        event.id = UUID.randomUUID();
        event.mosqueId = Objects.requireNonNull(mosqueId, "mosqueId");
        event.status = EventStatus.DRAFT;
        event.createdAt = Objects.requireNonNull(now, "now");
        event.createdBy = Objects.requireNonNull(actorId, "actorId");
        event.apply(details, actorId, now);
        return event;
    }

    public void update(EventDetails details, UUID actorId, Instant now) {
        requireNotCancelled();
        apply(details, actorId, now);
    }

    public void publish(UUID actorId, Instant now) {
        requireNotCancelled();
        if (status == EventStatus.PUBLISHED) {
            return;
        }
        status = EventStatus.PUBLISHED;
        if (publishedAt == null) {
            publishedAt = now;
        }
        touch(actorId, now);
    }

    public void unpublish(UUID actorId, Instant now) {
        requireNotCancelled();
        status = EventStatus.DRAFT;
        touch(actorId, now);
    }

    /** Only a published event can be cancelled; a draft is simply deleted. Final. */
    public void cancel(UUID actorId, Instant now) {
        if (status == EventStatus.CANCELLED) {
            return;
        }
        if (status != EventStatus.PUBLISHED) {
            throw new EventRuleViolationException("EVENT_NOT_PUBLISHED",
                    "Only a published event can be cancelled. Delete a draft instead.");
        }
        status = EventStatus.CANCELLED;
        cancelledAt = now;
        touch(actorId, now);
    }

    public boolean isPubliclyListed() {
        return status == EventStatus.PUBLISHED || status == EventStatus.CANCELLED;
    }

    /** Over when the end has passed, or the start for an event without an end (as in the agenda lists). */
    public boolean isOver(Instant now) {
        return !now.isBefore(endsAt != null ? endsAt : startsAt);
    }

    public boolean hasStarted(Instant now) {
        return !now.isBefore(startsAt);
    }

    private void requireNotCancelled() {
        if (status == EventStatus.CANCELLED) {
            throw new EventRuleViolationException("EVENT_CANCELLED", "This event has been cancelled.");
        }
    }

    private void apply(EventDetails details, UUID actorId, Instant now) {
        title = details.title();
        description = details.description();
        category = details.category();
        startsAt = details.startsAt();
        endsAt = details.endsAt();
        location = details.location();
        imageUrl = details.imageUrl();
        visibility = details.visibility();
        registrationEnabled = details.registrationEnabled();
        capacity = details.capacity();
        touch(actorId, now);
    }

    private void touch(UUID actorId, Instant now) {
        updatedBy = Objects.requireNonNull(actorId, "actorId");
        updatedAt = Objects.requireNonNull(now, "now");
    }

    public EventDetails details() {
        return new EventDetails(title, description, category, startsAt, endsAt, location, imageUrl, visibility,
                registrationEnabled, capacity);
    }

    public UUID getId() {
        return id;
    }

    public UUID getMosqueId() {
        return mosqueId;
    }

    public String getTitle() {
        return title;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public boolean isRegistrationEnabled() {
        return registrationEnabled;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public EventStatus getStatus() {
        return status;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
