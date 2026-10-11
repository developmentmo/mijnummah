package nl.mijnummah.notification.domain;

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

import nl.mijnummah.notification.PushContentType;
import nl.mijnummah.notification.PushNotificationView;
import nl.mijnummah.notification.PushRequest;
import nl.mijnummah.notification.PushStatus;

/** A push notification about one content item, with delivery counts. */
@Entity
@Table(name = "push_notification")
public class PushNotification {

    @Id
    private UUID id;

    @Column(name = "mosque_id", nullable = false, updatable = false)
    private UUID mosqueId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, updatable = false, length = 20)
    private PushContentType contentType;

    @Column(name = "content_id", nullable = false, updatable = false)
    private UUID contentId;

    @Column(name = "title", nullable = false, updatable = false, length = PushRequest.MAX_TITLE_LENGTH)
    private String title;

    @Column(name = "body", nullable = false, updatable = false, length = PushRequest.MAX_BODY_LENGTH)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PushStatus status;

    @Column(name = "recipient_count", nullable = false)
    private int recipientCount;

    @Column(name = "delivered_count", nullable = false)
    private int deliveredCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected PushNotification() {
        // for JPA
    }

    /** Accepted for sending to {@code recipientCount} devices. */
    public static PushNotification start(PushRequest request, int recipientCount, UUID createdBy, Instant now) {
        PushNotification notification = new PushNotification();
        notification.id = UUID.randomUUID();
        notification.mosqueId = request.mosqueId();
        notification.contentType = request.contentType();
        notification.contentId = request.contentId();
        notification.title = request.title();
        notification.body = request.body();
        notification.status = PushStatus.SENDING;
        notification.recipientCount = recipientCount;
        notification.createdBy = Objects.requireNonNull(createdBy, "createdBy");
        notification.createdAt = Objects.requireNonNull(now, "now");
        if (recipientCount == 0) {
            notification.status = PushStatus.SENT;
            notification.completedAt = now;
        }
        return notification;
    }

    public void complete(int delivered, int failed, Instant now) {
        if (delivered + failed > recipientCount) {
            throw new IllegalArgumentException("More results than recipients");
        }
        deliveredCount = delivered;
        failedCount = failed;
        status = PushStatus.SENT;
        completedAt = now;
    }

    /** The push service became unusable; every device not reached yet counts as failed. */
    public void fail(int delivered, Instant now) {
        deliveredCount = delivered;
        failedCount = recipientCount - delivered;
        status = PushStatus.FAILED;
        completedAt = now;
    }

    public PushNotificationView view() {
        return new PushNotificationView(id, mosqueId, contentType, contentId, title, body, status, recipientCount,
                deliveredCount, failedCount, createdAt, completedAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getMosqueId() {
        return mosqueId;
    }

    public PushContentType getContentType() {
        return contentType;
    }

    public UUID getContentId() {
        return contentId;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }
}
