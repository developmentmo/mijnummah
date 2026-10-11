package nl.mijnummah.announcement.infrastructure;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nl.mijnummah.announcement.domain.Announcement;
import nl.mijnummah.announcement.domain.AnnouncementStatus;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    /** Tenant-scoped lookup: an announcement of another mosque is never returned. */
    Optional<Announcement> findByIdAndMosqueId(UUID id, UUID mosqueId);

    Page<Announcement> findByMosqueId(UUID mosqueId, Pageable pageable);

    Page<Announcement> findByMosqueIdAndStatus(UUID mosqueId, AnnouncementStatus status, Pageable pageable);

    /** Published, not expired. Featured items first, then newest first. */
    @Query("""
            select a from Announcement a
            where a.mosqueId = :mosqueId
              and a.status = nl.mijnummah.announcement.domain.AnnouncementStatus.PUBLISHED
              and (a.expiresAt is null or a.expiresAt > :now)
            order by a.featured desc, a.publishedAt desc, a.id""")
    Page<Announcement> findPublic(@Param("mosqueId") UUID mosqueId, @Param("now") Instant now, Pageable pageable);
}
