package nl.mijnummah.notification.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import nl.mijnummah.notification.PushContentType;
import nl.mijnummah.notification.domain.PushNotification;

public interface PushNotificationRepository extends JpaRepository<PushNotification, UUID> {

    Page<PushNotification> findByMosqueId(UUID mosqueId, Pageable pageable);

    Optional<PushNotification> findByMosqueIdAndContentTypeAndContentId(UUID mosqueId, PushContentType contentType,
                                                                        UUID contentId);

    boolean existsByContentTypeAndContentId(PushContentType contentType, UUID contentId);
}
