package nl.mijnummah.announcement.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.announcement.domain.Announcement;
import nl.mijnummah.announcement.domain.AnnouncementDetails;
import nl.mijnummah.announcement.domain.AnnouncementStatus;
import nl.mijnummah.announcement.infrastructure.AnnouncementRepository;
import nl.mijnummah.common.audit.AuditEvent;
import nl.mijnummah.common.error.NotFoundException;
import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.mosque.MosqueAccessService;
import nl.mijnummah.mosque.MosqueDirectory;
import nl.mijnummah.mosque.MosquePermission;
import nl.mijnummah.mosque.MosqueRef;
import nl.mijnummah.notification.PushContentNotPublishedException;
import nl.mijnummah.notification.PushContentType;
import nl.mijnummah.notification.PushNotificationView;
import nl.mijnummah.notification.PushNotifications;
import nl.mijnummah.notification.PushRequest;
import nl.mijnummah.notification.PushText;
import nl.mijnummah.storage.ImageStorage;

/** Public announcements of active mosques, and their management ({@link MosquePermission#MANAGE_CONTENT}). */
@Service
public class AnnouncementService {

    private final AnnouncementRepository announcements;
    private final MosqueDirectory mosques;
    private final MosqueAccessService access;
    private final PushNotifications push;
    private final ImageStorage images;
    private final ApplicationEventPublisher audit;
    private final Clock clock;

    public AnnouncementService(AnnouncementRepository announcements, MosqueDirectory mosques,
                               MosqueAccessService access, PushNotifications push, ImageStorage images,
                               ApplicationEventPublisher audit, Clock clock) {
        this.announcements = announcements;
        this.mosques = mosques;
        this.access = access;
        this.push = push;
        this.images = images;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<AnnouncementView> publicList(String mosqueSlug, Pageable pageable) {
        MosqueRef mosque = mosques.requireActiveBySlug(mosqueSlug);
        Instant now = Instant.now(clock);
        return announcements.findPublic(mosque.id(), now, pageable).map(a -> AnnouncementView.of(a, now));
    }

    /**
     * News across mosques for the central app: all active mosques, or only those among {@code mosqueSlugs} when
     * given (unknown and inactive slugs are ignored).
     */
    @Transactional(readOnly = true)
    public Page<AnnouncementView> publicListAcross(Collection<String> mosqueSlugs, Pageable pageable) {
        Set<UUID> mosqueIds = mosqueSlugs == null || mosqueSlugs.isEmpty()
                ? mosques.activeIds()
                : mosques.findActiveBySlugs(mosqueSlugs).keySet();
        if (mosqueIds.isEmpty()) {
            return Page.empty(pageable);
        }
        Instant now = Instant.now(clock);
        return announcements.findPublicIn(mosqueIds, now, pageable).map(a -> AnnouncementView.of(a, now));
    }

    /** One public announcement of any active mosque, e.g. opened from a push notification. */
    @Transactional(readOnly = true)
    public AnnouncementView publicGet(UUID announcementId) {
        Instant now = Instant.now(clock);
        return announcements.findById(announcementId)
                .filter(a -> a.getStatus() == AnnouncementStatus.PUBLISHED && !a.isExpired(now))
                .filter(a -> !mosques.findActiveByIds(List.of(a.getMosqueId())).isEmpty())
                .map(a -> AnnouncementView.of(a, now))
                .orElseThrow(AnnouncementService::notFound);
    }

    @Transactional(readOnly = true)
    public AnnouncementView publicGet(String mosqueSlug, UUID announcementId) {
        MosqueRef mosque = mosques.requireActiveBySlug(mosqueSlug);
        Instant now = Instant.now(clock);
        return announcements.findByIdAndMosqueId(announcementId, mosque.id())
                .filter(a -> a.getStatus() == AnnouncementStatus.PUBLISHED && !a.isExpired(now))
                .map(a -> AnnouncementView.of(a, now))
                .orElseThrow(AnnouncementService::notFound);
    }

    @Transactional(readOnly = true)
    public Page<AnnouncementView> list(AuthenticatedUser actor, UUID mosqueId, AnnouncementStatus status,
                                       Pageable pageable) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        Instant now = Instant.now(clock);
        Page<Announcement> page = status == null
                ? announcements.findByMosqueId(mosqueId, pageable)
                : announcements.findByMosqueIdAndStatus(mosqueId, status, pageable);
        return page.map(a -> AnnouncementView.of(a, now));
    }

    @Transactional(readOnly = true)
    public AnnouncementView get(AuthenticatedUser actor, UUID mosqueId, UUID announcementId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        return AnnouncementView.of(load(mosqueId, announcementId), Instant.now(clock));
    }

    @Transactional
    public AnnouncementView create(AuthenticatedUser actor, UUID mosqueId, AnnouncementDetails details) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        requireOwnImage(details, mosqueId);
        Instant now = Instant.now(clock);
        Announcement announcement = announcements.save(Announcement.draft(mosqueId, details, actor.id(), now));
        audit.publishEvent(AuditEvent.of(actor.id(), mosqueId, "ANNOUNCEMENT_CREATED", "ANNOUNCEMENT", announcement.getId()));
        return AnnouncementView.of(announcement, now);
    }

    @Transactional
    public AnnouncementView update(AuthenticatedUser actor, UUID mosqueId, UUID announcementId,
                                   AnnouncementDetails details) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        requireOwnImage(details, mosqueId);
        Announcement announcement = load(mosqueId, announcementId);
        Instant now = Instant.now(clock);
        announcement.update(details, actor.id(), now);
        audit.publishEvent(AuditEvent.of(actor.id(), mosqueId, "ANNOUNCEMENT_UPDATED", "ANNOUNCEMENT", announcementId));
        return AnnouncementView.of(announcement, now);
    }

    /** Makes the announcement visible in the app. Does not send a push notification (see {@link #sendPush}). */
    @Transactional
    public AnnouncementView publish(AuthenticatedUser actor, UUID mosqueId, UUID announcementId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        Announcement announcement = load(mosqueId, announcementId);
        Instant now = Instant.now(clock);
        announcement.publish(actor.id(), now);
        audit.publishEvent(AuditEvent.of(actor.id(), mosqueId, "ANNOUNCEMENT_PUBLISHED", "ANNOUNCEMENT", announcementId));
        return AnnouncementView.of(announcement, now);
    }

    @Transactional
    public AnnouncementView unpublish(AuthenticatedUser actor, UUID mosqueId, UUID announcementId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        Announcement announcement = load(mosqueId, announcementId);
        Instant now = Instant.now(clock);
        announcement.unpublish(actor.id(), now);
        audit.publishEvent(AuditEvent.of(actor.id(), mosqueId, "ANNOUNCEMENT_UNPUBLISHED", "ANNOUNCEMENT", announcementId));
        return AnnouncementView.of(announcement, now);
    }

    /**
     * Sends a push notification about a published, unexpired announcement to the followers who want announcements.
     * By default the title and text of the announcement itself.
     */
    @Transactional
    public PushNotificationView sendPush(AuthenticatedUser actor, UUID mosqueId, UUID announcementId, PushText text) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        Announcement announcement = load(mosqueId, announcementId);
        if (announcement.getStatus() != AnnouncementStatus.PUBLISHED || announcement.isExpired(Instant.now(clock))) {
            throw new PushContentNotPublishedException("Only a published announcement that has not expired can be "
                    + "sent as a push notification.");
        }
        AnnouncementDetails details = announcement.details();
        return push.send(actor, new PushRequest(mosqueId, PushContentType.ANNOUNCEMENT, announcementId,
                text.titleOr(details.title()), text.bodyOr(details.body())));
    }

    @Transactional
    public void delete(AuthenticatedUser actor, UUID mosqueId, UUID announcementId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        announcements.delete(load(mosqueId, announcementId));
        audit.publishEvent(AuditEvent.of(actor.id(), mosqueId, "ANNOUNCEMENT_DELETED", "ANNOUNCEMENT", announcementId));
    }

    private Announcement load(UUID mosqueId, UUID announcementId) {
        return announcements.findByIdAndMosqueId(announcementId, mosqueId).orElseThrow(AnnouncementService::notFound);
    }

    private static NotFoundException notFound() {
        return new NotFoundException("ANNOUNCEMENT_NOT_FOUND", "Announcement not found.");
    }

    private void requireOwnImage(AnnouncementDetails details, UUID mosqueId) {
        if (details.imageId() != null) {
            images.requireOwnedBy(details.imageId(), mosqueId);
        }
    }
}
