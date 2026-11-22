package nl.mijnummah.notification.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.common.audit.AuditEvent;
import nl.mijnummah.follow.FollowerDirectory;
import nl.mijnummah.follow.NotificationTopic;
import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.identity.UserDirectory;
import nl.mijnummah.mosque.MosqueAccessService;
import nl.mijnummah.mosque.MosqueDirectory;
import nl.mijnummah.mosque.MosquePermission;
import nl.mijnummah.notification.PushAlreadySentException;
import nl.mijnummah.notification.PushContentType;
import nl.mijnummah.notification.PushDisabledForMosqueException;
import nl.mijnummah.notification.PushNotConfiguredException;
import nl.mijnummah.notification.PushNotificationView;
import nl.mijnummah.notification.PushNotifications;
import nl.mijnummah.notification.PushRequest;
import nl.mijnummah.notification.PushTarget;
import nl.mijnummah.notification.domain.PushDevice;
import nl.mijnummah.notification.domain.PushNotification;
import nl.mijnummah.notification.infrastructure.PushDeviceRepository;
import nl.mijnummah.notification.infrastructure.PushNotificationRepository;

/**
 * Decides who receives a push notification and records it; {@link PushDispatcher} delivers it.
 * <p>
 * The audience is always derived from the mosque in the route (after the access check): active followers of that
 * mosque who did not switch the content's topic off, and their devices in that mosque's app. Followers of other
 * mosques, devices in the apps of other mosques, and people who do not follow, are never addressed.
 */
@Service
public class PushNotificationService implements PushNotifications {

    private final PushNotificationRepository notifications;
    private final PushDeviceRepository devices;
    private final FollowerDirectory followers;
    private final UserDirectory users;
    private final MosqueAccessService access;
    private final MosqueDirectory mosques;
    private final PushGateway gateway;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public PushNotificationService(PushNotificationRepository notifications, PushDeviceRepository devices,
                                   FollowerDirectory followers, UserDirectory users, MosqueAccessService access,
                                   MosqueDirectory mosques, PushGateway gateway, ApplicationEventPublisher events, Clock clock) {
        this.notifications = notifications;
        this.devices = devices;
        this.followers = followers;
        this.users = users;
        this.access = access;
        this.mosques = mosques;
        this.gateway = gateway;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PushNotificationView send(AuthenticatedUser actor, PushRequest request) {
        requireSendable(actor, request.mosqueId());
        if (notifications.existsByContentTypeAndContentId(request.contentType(), request.contentId())) {
            throw new PushAlreadySentException();
        }
        List<String> tokens = tokens(request.mosqueId(), Set.of(request.contentType().topic()));
        PushNotification notification = PushNotification.start(request, tokens.size(), actor.id(),
                Instant.now(clock));
        try {
            notifications.saveAndFlush(notification);
        } catch (DataIntegrityViolationException ex) {
            // sent concurrently (double click): the unique constraint keeps it at one
            throw new PushAlreadySentException();
        }
        return accepted(actor, notification, tokens);
    }

    /**
     * Sends a free message (title and text written by the administrator) to the chosen followers of the mosque.
     * Same rules as {@link #send}: ADMIN only, push configured and switched on for the mosque.
     */
    @Transactional
    public PushNotificationView sendMessage(AuthenticatedUser actor, UUID mosqueId, String title, String body,
                                            PushTarget audience) {
        requireSendable(actor, mosqueId);
        List<String> tokens = tokens(mosqueId, audience.topics());
        PushNotification notification = PushNotification.message(mosqueId,
                PushRequest.shorten(title.strip(), PushRequest.MAX_TITLE_LENGTH),
                PushRequest.shorten(body.strip(), PushRequest.MAX_BODY_LENGTH), audience, tokens.size(), actor.id(),
                Instant.now(clock));
        notifications.saveAndFlush(notification);
        return accepted(actor, notification, tokens);
    }

    private void requireSendable(AuthenticatedUser actor, UUID mosqueId) {
        access.require(actor, mosqueId, MosquePermission.SEND_NOTIFICATIONS);
        if (!gateway.configured()) {
            throw new PushNotConfiguredException();
        }
        if (!mosques.settings(mosqueId).pushEnabled()) {
            throw new PushDisabledForMosqueException();
        }
    }

    private List<String> tokens(UUID mosqueId, Set<NotificationTopic> topics) {
        Set<UUID> recipients = recipients(mosqueId, topics);
        return recipients.isEmpty() ? List.of() : devices.findByUserIdIn(recipients).stream()
                .map(PushDevice::getToken)
                .toList();
    }

    private PushNotificationView accepted(AuthenticatedUser actor, PushNotification notification, List<String> tokens) {
        events.publishEvent(AuditEvent.of(actor.id(), notification.getMosqueId(), "PUSH_SENT", "PUSH_NOTIFICATION",
                notification.getId()));
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
        return audience(actor, mosqueId, Set.of(contentType.topic()));
    }

    /** How many people and devices a free message to this audience would reach now. */
    @Transactional(readOnly = true)
    public PushAudience audience(AuthenticatedUser actor, UUID mosqueId, PushTarget target) {
        return audience(actor, mosqueId, target.topics());
    }

    private PushAudience audience(AuthenticatedUser actor, UUID mosqueId, Set<NotificationTopic> topics) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        Set<UUID> recipients = recipients(mosqueId, topics);
        long deviceCount = recipients.isEmpty() ? 0 : devices.countByUserIdIn(recipients);
        return new PushAudience(gateway.configured(), gateway.simulated(),
                mosques.settings(mosqueId).pushEnabled(), recipients.size(),
                Math.toIntExact(deviceCount));
    }

    /** Active followers who receive notifications about at least one of the topics. */
    private Set<UUID> recipients(UUID mosqueId, Set<NotificationTopic> topics) {
        Set<UUID> followerIds = new HashSet<>();
        for (NotificationTopic topic : topics) {
            followerIds.addAll(followers.followersToNotify(mosqueId, topic));
        }
        return followerIds.isEmpty() ? Set.of() : users.activeAmong(followerIds);
    }

    /** The data tells the app which screen to open when the notification is tapped. */
    static PushMessage message(PushNotification notification) {
        String type = notification.getContentType().name().toLowerCase(Locale.ROOT);
        String mosqueId = notification.getMosqueId().toString();
        if (notification.getContentId() == null) {
            // A free message: the app opens the mosque.
            return new PushMessage(notification.getTitle(), notification.getBody(),
                    Map.of("type", type, "mosqueId", mosqueId));
        }
        return new PushMessage(notification.getTitle(), notification.getBody(), Map.of(
                "type", type,
                "id", notification.getContentId().toString(),
                "mosqueId", mosqueId));
    }
}
