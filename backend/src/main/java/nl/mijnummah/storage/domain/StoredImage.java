package nl.mijnummah.storage.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Metadata of an uploaded image; the file is in the image store under its ID. Never changed after upload. */
@Entity
@Table(name = "image")
public class StoredImage {

    @Id
    private UUID id;

    @Column(name = "mosque_id", nullable = false, updatable = false)
    private UUID mosqueId;

    @Column(name = "content_type", nullable = false, updatable = false, length = 20)
    private String contentType;

    @Column(name = "width", nullable = false, updatable = false)
    private int width;

    @Column(name = "height", nullable = false, updatable = false)
    private int height;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    @Column(name = "uploaded_by", updatable = false)
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StoredImage() {
        // for JPA
    }

    public static StoredImage of(UUID mosqueId, UUID uploadedBy, EncodedImage image, Instant now) {
        StoredImage stored = new StoredImage();
        stored.id = UUID.randomUUID();
        stored.mosqueId = mosqueId;
        stored.uploadedBy = uploadedBy;
        stored.contentType = image.contentType();
        stored.width = image.width();
        stored.height = image.height();
        stored.sizeBytes = image.bytes().length;
        stored.createdAt = now;
        return stored;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMosqueId() {
        return mosqueId;
    }

    public String getContentType() {
        return contentType;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
