package nl.mijnummah.notification.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.follow.FollowerDirectory;
import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.mosque.MosqueCard;
import nl.mijnummah.mosque.MosqueDirectory;
import nl.mijnummah.mosque.MosqueRef;
import nl.mijnummah.notification.PushStatus;
import nl.mijnummah.notification.domain.PushNotification;
import nl.mijnummah.notification.infrastructure.PushNotificationRepository;

/**
 * The notifications screen of the app: what the mosques the user follows sent recently, also when the push did
 * not reach this phone (switched off, other device).
 */
@Service
public class NotificationInboxService {

    /** How far back the screen looks. */
    public static final Duration WINDOW = Duration.ofDays(30);

    private final PushNotificationRepository notifications;
    private final FollowerDirectory follows;
    private final MosqueDirectory mosques;
    private final Clock clock;

    public NotificationInboxService(PushNotificationRepository notifications, FollowerDirectory follows,
                                    MosqueDirectory mosques, Clock clock) {
        this.notifications = notifications;
        this.follows = follows;
        this.mosques = mosques;
        this.clock = clock;
    }

    /**
     * @param type      the kind of screen to open, as in the push data: {@code announcement}, {@code event},
     *                  {@code media} or {@code message}
     * @param contentId null for a free message
     */
    public record InboxItem(UUID id, MosqueCard mosque, String type, UUID contentId, String title, String body,
                            Instant sentAt) {
    }

    @Transactional(readOnly = true)
    public Page<InboxItem> mine(AuthenticatedUser actor, Pageable pageable) {
        Map<UUID, MosqueRef> followed = mosques.findActiveByIds(follows.followedMosqueIds(actor.id()));
        if (followed.isEmpty()) {
            return Page.empty(pageable);
        }
        Pageable newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
        return notifications.findByMosqueIdInAndStatusNotAndCreatedAtAfter(followed.keySet(), PushStatus.FAILED,
                        Instant.now(clock).minus(WINDOW), newestFirst)
                .map(n -> item(n, followed.get(n.getMosqueId())));
    }

    private static InboxItem item(PushNotification n, MosqueRef mosque) {
        PushMessage message = PushNotificationService.message(n);
        return new InboxItem(n.getId(), MosqueCard.of(mosque), message.data().get("type"), n.getContentId(),
                n.getTitle(), n.getBody(), n.getCreatedAt());
    }
}
