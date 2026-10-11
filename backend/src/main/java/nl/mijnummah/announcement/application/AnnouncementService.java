package nl.mijnummah.announcement.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.announcement.domain.Announcement;
import nl.mijnummah.announcement.domain.AnnouncementDetails;
import nl.mijnummah.announcement.domain.AnnouncementStatus;
import nl.mijnummah.announcement.infrastructure.AnnouncementRepository;
import nl.mijnummah.common.error.NotFoundException;
import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.mosque.MosqueAccessService;
import nl.mijnummah.mosque.MosqueDirectory;
import nl.mijnummah.mosque.MosquePermission;
import nl.mijnummah.mosque.MosqueRef;

/** Public announcements of active mosques, and their management ({@link MosquePermission#MANAGE_CONTENT}). */
@Service
public class AnnouncementService {

    private final AnnouncementRepository announcements;
    private final MosqueDirectory mosques;
    private final MosqueAccessService access;
    private final Clock clock;

    public AnnouncementService(AnnouncementRepository announcements, MosqueDirectory mosques,
                               MosqueAccessService access, Clock clock) {
        this.announcements = announcements;
        this.mosques = mosques;
        this.access = access;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<AnnouncementView> publicList(String mosqueSlug, Pageable pageable) {
        MosqueRef mosque = mosques.requireActiveBySlug(mosqueSlug);
        Instant now = Instant.now(clock);
        return announcements.findPublic(mosque.id(), now, pageable).map(a -> AnnouncementView.of(a, now));
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
        Instant now = Instant.now(clock);
        return AnnouncementView.of(announcements.save(Announcement.draft(mosqueId, details, actor.id(), now)), now);
    }

    @Transactional
    public AnnouncementView update(AuthenticatedUser actor, UUID mosqueId, UUID announcementId,
                                   AnnouncementDetails details) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        Announcement announcement = load(mosqueId, announcementId);
        Instant now = Instant.now(clock);
        announcement.update(details, actor.id(), now);
        return AnnouncementView.of(announcement, now);
    }

    /** Makes the announcement visible in the app. Does not send a push notification. */
    @Transactional
    public AnnouncementView publish(AuthenticatedUser actor, UUID mosqueId, UUID announcementId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        Announcement announcement = load(mosqueId, announcementId);
        Instant now = Instant.now(clock);
        announcement.publish(actor.id(), now);
        return AnnouncementView.of(announcement, now);
    }

    @Transactional
    public AnnouncementView unpublish(AuthenticatedUser actor, UUID mosqueId, UUID announcementId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        Announcement announcement = load(mosqueId, announcementId);
        Instant now = Instant.now(clock);
        announcement.unpublish(actor.id(), now);
        return AnnouncementView.of(announcement, now);
    }

    @Transactional
    public void delete(AuthenticatedUser actor, UUID mosqueId, UUID announcementId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        announcements.delete(load(mosqueId, announcementId));
    }

    private Announcement load(UUID mosqueId, UUID announcementId) {
        return announcements.findByIdAndMosqueId(announcementId, mosqueId).orElseThrow(AnnouncementService::notFound);
    }

    private static NotFoundException notFound() {
        return new NotFoundException("ANNOUNCEMENT_NOT_FOUND", "Announcement not found.");
    }
}
