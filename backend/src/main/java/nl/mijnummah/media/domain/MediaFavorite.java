package nl.mijnummah.media.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** A video that a user saved as a favourite (020). Removed with the user or the media item. */
@Entity
@Table(name = "media_favorite")
@IdClass(MediaFavorite.Key.class)
public class MediaFavorite {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Id
    @Column(name = "media_id", nullable = false, updatable = false)
    private UUID mediaId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MediaFavorite() {
    }

    public static MediaFavorite of(UUID userId, UUID mediaId, Instant now) {
        MediaFavorite favorite = new MediaFavorite();
        favorite.userId = userId;
        favorite.mediaId = mediaId;
        favorite.createdAt = now;
        return favorite;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getMediaId() {
        return mediaId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public record Key(UUID userId, UUID mediaId) implements Serializable {

        public Key() {
            this(null, null);
        }
    }
}
