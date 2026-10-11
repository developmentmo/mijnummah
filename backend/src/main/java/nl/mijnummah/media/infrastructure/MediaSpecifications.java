package nl.mijnummah.media.infrastructure;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import nl.mijnummah.media.domain.MediaCategory;
import nl.mijnummah.media.domain.MediaItem;
import nl.mijnummah.media.domain.MediaStatus;
import nl.mijnummah.media.domain.Visibility;

/** Composable filters for media lists. A null argument means "no filter". */
public final class MediaSpecifications {

    private MediaSpecifications() {
    }

    public static Specification<MediaItem> ofMosque(UUID mosqueId) {
        return (root, query, cb) -> cb.equal(root.get("mosqueId"), mosqueId);
    }

    public static Specification<MediaItem> status(MediaStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<MediaItem> visibilityIn(Collection<Visibility> visibilities) {
        return (root, query, cb) -> root.get("visibility").in(visibilities);
    }

    public static Specification<MediaItem> category(MediaCategory category) {
        return (root, query, cb) -> category == null ? null : cb.equal(root.get("category"), category);
    }

    /** Case-insensitive search in title and speaker. LIKE wildcards in the input are matched literally. */
    public static Specification<MediaItem> matches(String text) {
        if (text == null || text.isBlank()) {
            return Specification.unrestricted();
        }
        String pattern = "%" + text.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern, '\\'),
                cb.like(cb.lower(cb.coalesce(root.get("speaker"), "")), pattern, '\\'));
    }

    public static Specification<MediaItem> notId(UUID id) {
        return (root, query, cb) -> cb.notEqual(root.get("id"), id);
    }

    /** Same category, or (when known) the same speaker. */
    public static Specification<MediaItem> relatedTo(MediaCategory category, String speaker) {
        return (root, query, cb) -> speaker == null
                ? cb.equal(root.get("category"), category)
                : cb.or(cb.equal(root.get("category"), category), cb.equal(root.get("speaker"), speaker));
    }
}
