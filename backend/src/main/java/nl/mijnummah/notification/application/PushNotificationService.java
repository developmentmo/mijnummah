package nl.mijnummah.notification.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.follow.FollowerDirectory;
import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.identity.UserDirectory;
import nl.mijnummah.mosque.MosqueAccessService;
import nl.mijnummah.mosque.MosquePermission;
import nl.mijnummah.notification.PushAlreadySentException;
import nl.mijnummah.notification.PushContentType;
import nl.mijnummah.notification.PushNotConfiguredException;
import nl.mijnummah.notification.PushNotificationView;
import nl.mijnummah.notification.PushNotifications;
import nl.mijnummah.notification.PushRequest;
import nl.mijnummah.notification.domain.PushDevice;
import nl.mijnummah.notification.domain.PushNotification;
import nl.mijnummah.notification.infrastructure.PushDeviceRepository;
import nl.mijnummah.notification.infrastructure.PushNotificationRepository;

/**
 * Decides who receives a push notification and records it; {@link PushDispatcher} delivers it.
 * <p>
 * The audience is always derived from the mosque in the route (after the access check): active followers of that
 * mosque who did not switch the content's topic off, and their registered devices. Followers of other mosques, and
 * people who do not follow, are never addressed.
 */
@Service
public class PushNotificationService implements PushNotifications {

    private final PushNotificationRepository notifications;
    private final PushDeviceRepository devices;
    private final FollowerDirectory followers;
    private final UserDirectory users;
    private final MosqueAccessService access;
    private final PushGateway gateway;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public PushNotificationService(PushNotificationRepository notifications, PushDeviceRepository devices,
                                   FollowerDirectory followers, UserDirectory users, MosqueAccessService access,
                                   PushGateway gateway, ApplicationEventPublisher events, Clock clock) {
        this.notifications = notifications;
        this.devices = devices;
        this.followers = followers;
        this.users = users;
        this.access = access;
        this.gateway = gateway;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PushNotificationView send(AuthenticatedUser actor, PushRequest request) {
        access.require(actor, request.mosqueId(), MosquePermission.SEND_NOTIFICATIONS);
        if (!gateway.configured()) {
            throw new PushNotConfiguredException();
        }
        if (notifications.existsByContentTypeAndContentId(request.contentType(), request.contentId())) {
            throw new PushAlreadySentException();
        }
        List<String> tokens = devices.findByUserIdIn(recipients(request.mosqueId(), request.contentType())).stream()
                .map(PushDevice::getToken)
                .toList();
        PushNotification notification = PushNotification.start(request, tokens.size(), actor.id(),
                Instant.now(clock));
        try {
            notifications.saveAndFlush(notification);
        } catch (DataIntegrityViolationException ex) {
            // sent concurrently (double click): the unique constraint keeps it at one
            throw new PushAlreadySentException();
        }
        if (!tokens.isEmpty()) {
            events.publishEvent(new PushNotificationAccepted(notification.getId(), message(notification), tokens));
        }
        return notification.view();
    }

    /** History of the mosque's push notifications, newest first. */
    @Transactional(readOnly = true)
    public Page<PushNotificationView> list(AuthenticatedUser actor, UUID mosqueId, Pageable pageable) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        return notifications.findByMosqueId(mosqueId, pageable).map(PushNotification::view);
    }

    /** The notification sent about one content item, if any. */
    @Transactional(readOnly = true)
    public Optional<PushNotificationView> forContent(AuthenticatedUser actor, UUID mosqueId,
                                                     PushContentType contentType, UUID contentId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        return notifications.findByMosqueIdAndContentTypeAndContentId(mosqueId, contentType, contentId)
                .map(PushNotification::view);
    }

    /** How many people and devices a notification of this type would reach now. */
    @Transactional(readOnly = true)
    public PushAudience audience(AuthenticatedUser actor, UUID mosqueId, PushContentType contentType) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        Set<UUID> recipients = recipients(mosqueId, contentType);
        long deviceCount = recipients.isEmpty() ? 0 : devices.countByUserIdIn(recipients);
        return new PushAudience(gateway.configured(), gateway.simulated(), recipients.size(),
                Math.toIntExact(deviceCount));
    }

    private Set<UUID> recipients(UUID mosqueId, PushContentType contentType) {
        return users.activeAmong(followers.followersToNotify(mosqueId, contentType.topic()));
    }

    /** The data tells the app which screen to open when the notification is tapped. */
    static PushMessage message(PushNotification notification) {
        return new PushMessage(notification.getTitle(), notification.getBody(), Map.of(
                "type", notification.getContentType().name().toLowerCase(Locale.ROOT),
                "id", notification.getContentId().toString(),
                "mosqueId", notification.getMosqueId().toString()));
    }
}
