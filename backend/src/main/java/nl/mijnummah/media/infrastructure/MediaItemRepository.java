package nl.mijnummah.media.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import nl.mijnummah.media.domain.MediaItem;
import nl.mijnummah.media.domain.VideoProviderType;

public interface MediaItemRepository extends JpaRepository<MediaItem, UUID>, JpaSpecificationExecutor<MediaItem> {

    /** Tenant-scoped lookup: an item of another mosque is never returned. */
    Optional<MediaItem> findByIdAndMosqueId(UUID id, UUID mosqueId);

    Optional<MediaItem> findByMosqueIdAndVideoProviderAndVideoId(UUID mosqueId, VideoProviderType provider,
                                                                 String videoId);
}
