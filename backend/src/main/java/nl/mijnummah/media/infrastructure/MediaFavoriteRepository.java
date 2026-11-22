package nl.mijnummah.media.infrastructure;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nl.mijnummah.media.domain.MediaFavorite;
import nl.mijnummah.media.domain.MediaItem;

public interface MediaFavoriteRepository extends JpaRepository<MediaFavorite, MediaFavorite.Key> {

    /**
     * The user's favourites that are live now in one of these mosques, most recently saved first. A signed-in user
     * sees every visibility, so it is not filtered.
     */
    @Query(value = """
            select m from MediaFavorite f, MediaItem m
            where f.mediaId = m.id and f.userId = :userId and m.mosqueId in :mosqueIds
              and m.status = nl.mijnummah.media.domain.MediaStatus.PUBLISHED and m.publishedAt <= :now
            order by f.createdAt desc, m.id""",
            countQuery = """
            select count(f) from MediaFavorite f, MediaItem m
            where f.mediaId = m.id and f.userId = :userId and m.mosqueId in :mosqueIds
              and m.status = nl.mijnummah.media.domain.MediaStatus.PUBLISHED and m.publishedAt <= :now""")
    Page<MediaItem> findLiveFavorites(@Param("userId") UUID userId, @Param("mosqueIds") Collection<UUID> mosqueIds,
                                      @Param("now") Instant now, Pageable pageable);

    @Query("select f.mediaId from MediaFavorite f where f.userId = :userId")
    List<UUID> findMediaIdsByUserId(@Param("userId") UUID userId);
}
