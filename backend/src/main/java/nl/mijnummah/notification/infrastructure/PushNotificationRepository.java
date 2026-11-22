package nl.mijnummah.notification.infrastructure;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import nl.mijnummah.notification.PushContentType;
import nl.mijnummah.notification.PushStatus;
import nl.mijnummah.notification.domain.PushNotification;

public interface PushNotificationRepository extends JpaRepository<PushNotification, UUID> {

    Page<PushNotification> findByMosqueId(UUID mosqueId, Pageable pageable);

    Optional<PushNotification> findByMosqueIdAndContentTypeAndContentId(UUID mosqueId, PushContentType contentType,
                                                                        UUID contentId);

    boolean existsByContentTypeAndContentId(PushContentType contentType, UUID contentId);

    /** For the inbox in the app: notifications of these mosques since a moment, in a status other than this one. */
    Page<PushNotification> findByMosqueIdInAndStatusNotAndCreatedAtAfter(Collection<UUID> mosqueIds, PushStatus status,
                                                                          Instant since, Pageable pageable);

    List<PushNotification> findByStatusAndCreatedAtBefore(PushStatus status, Instant cutoff);
}
