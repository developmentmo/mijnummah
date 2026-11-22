package nl.mijnummah.media.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.media.domain.MediaFavorite;
import nl.mijnummah.media.domain.MediaItem;
import nl.mijnummah.media.infrastructure.MediaFavoriteRepository;
import nl.mijnummah.mosque.MosqueDirectory;

/** Favourite videos of the signed-in user; only about the caller's own favourites. */
@Service
public class MediaFavoriteService {

    private final MediaFavoriteRepository favorites;
    private final PublicMediaService publicMedia;
    private final MediaViews views;
    private final MosqueDirectory mosques;
    private final Clock clock;

    public MediaFavoriteService(MediaFavoriteRepository favorites, PublicMediaService publicMedia, MediaViews views,
                                MosqueDirectory mosques, Clock clock) {
        this.favorites = favorites;
        this.publicMedia = publicMedia;
        this.views = views;
        this.mosques = mosques;
        this.clock = clock;
    }

    /** Idempotent. Only a video the caller can see now (published, live, active mosque) can be saved. */
    @Transactional
    public void add(AuthenticatedUser actor, UUID mediaId) {
        MediaItem item = publicMedia.loadAny(mediaId, actor);
        var key = new MediaFavorite.Key(actor.id(), item.getId());
        if (!favorites.existsById(key)) {
            favorites.save(MediaFavorite.of(actor.id(), item.getId(), Instant.now(clock)));
        }
    }

    /** Idempotent; also for a video that is no longer visible. */
    @Transactional
    public void remove(AuthenticatedUser actor, UUID mediaId) {
        favorites.deleteById(new MediaFavorite.Key(actor.id(), mediaId));
    }

    /** Favourites that can be watched now, most recently saved first. */
    @Transactional(readOnly = true)
    public Page<MediaView> list(AuthenticatedUser actor, Pageable pageable) {
        Set<UUID> active = mosques.activeIds();
        if (active.isEmpty()) {
            return Page.empty(pageable);
        }
        Page<MediaItem> page = favorites.findLiveFavorites(actor.id(), active, Instant.now(clock), pageable);
        return new PageImpl<>(views.of(page.getContent()), page.getPageable(), page.getTotalElements());
    }

    /** IDs of all saved videos, so the app can show the favourite state everywhere. */
    @Transactional(readOnly = true)
    public List<UUID> ids(AuthenticatedUser actor) {
        return favorites.findMediaIdsByUserId(actor.id());
    }
}
