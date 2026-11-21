package nl.mijnummah.mosque.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.common.audit.AuditEvent;
import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.mosque.MosqueAccessDeniedException;
import nl.mijnummah.mosque.MosqueAccessService;
import nl.mijnummah.mosque.MosquePermission;
import nl.mijnummah.mosque.MosqueRole;
import nl.mijnummah.mosque.MosqueStatus;
import nl.mijnummah.mosque.domain.Mosque;
import nl.mijnummah.mosque.domain.MosqueAdministrator;
import nl.mijnummah.mosque.domain.MosqueDetails;
import nl.mijnummah.mosque.infrastructure.MosqueAdministratorRepository;
import nl.mijnummah.mosque.infrastructure.MosqueRepository;

@Service
public class MosqueService {

    private final MosqueRepository mosques;
    private final MosqueAdministratorRepository administrators;
    private final MosqueAccessService access;
    private final ApplicationEventPublisher audit;
    private final Clock clock;

    public MosqueService(MosqueRepository mosques, MosqueAdministratorRepository administrators,
                         MosqueAccessService access, ApplicationEventPublisher audit, Clock clock) {
        this.mosques = mosques;
        this.administrators = administrators;
        this.access = access;
        this.audit = audit;
        this.clock = clock;
    }

    /** Public profile of an active mosque. */
    @Transactional(readOnly = true)
    public MosqueView publicProfile(String slug) {
        return mosques.findBySlug(slug).filter(Mosque::isActive).map(mosque -> MosqueView.of(mosque, null))
                .orElseThrow(MosqueNotFound::exception);
    }

    /** All active mosques, e.g. for a "discover" screen. */
    @Transactional(readOnly = true)
    public List<MosqueView> publicList() {
        return mosques.findAll().stream()
                .filter(Mosque::isActive)
                .sorted(Comparator.comparing(Mosque::getName, String.CASE_INSENSITIVE_ORDER))
                .map(mosque -> MosqueView.of(mosque, null))
                .toList();
    }

    @Transactional
    public MosqueView create(AuthenticatedUser actor, String slug, MosqueDetails details) {
        if (!actor.isPlatformAdmin()) {
            throw new MosqueAccessDeniedException();
        }
        Mosque mosque = createUnchecked(slug, details);
        audit.publishEvent(AuditEvent.of(actor.id(), mosque.getId(), "MOSQUE_CREATED", "MOSQUE", mosque.getId()));
        return MosqueView.of(mosque, null);
    }

    /** Without an actor: only for the startup bootstrap of the first mosque. */
    @Transactional
    public Mosque createUnchecked(String slug, MosqueDetails details) {
        if (mosques.existsBySlug(slug)) {
            throw new SlugAlreadyTakenException();
        }
        Mosque mosque = Mosque.create(slug, details, Instant.now(clock));
        try {
            return mosques.saveAndFlush(mosque);
        } catch (DataIntegrityViolationException ex) {
            throw new SlugAlreadyTakenException();
        }
    }

    @Transactional(readOnly = true)
    public boolean exists(String slug) {
        return mosques.existsBySlug(slug);
    }

    /**
     * The mosques the caller administers, with their own role. A blocked mosque is left out, except for platform
     * administrators: its own administrators cannot manage it.
     */
    @Transactional(readOnly = true)
    public List<MosqueView> administered(AuthenticatedUser actor) {
        Map<UUID, MosqueRole> roles = administrators.findByUserId(actor.id()).stream()
                .collect(Collectors.toMap(MosqueAdministrator::getMosqueId, MosqueAdministrator::getRole));
        List<Mosque> visible = actor.isPlatformAdmin()
                ? mosques.findAll()
                : mosques.findAllById(roles.keySet()).stream().filter(mosque -> !mosque.isBlocked()).toList();
        return visible.stream()
                .sorted(Comparator.comparing(Mosque::getName, String.CASE_INSENSITIVE_ORDER))
                .map(mosque -> MosqueView.of(mosque, roles.get(mosque.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public MosqueView get(AuthenticatedUser actor, UUID mosqueId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        return view(actor, load(mosqueId));
    }

    @Transactional
    public MosqueView update(AuthenticatedUser actor, UUID mosqueId, MosqueDetails details) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_MOSQUE);
        Mosque mosque = load(mosqueId);
        mosque.update(details, Instant.now(clock));
        audit.publishEvent(AuditEvent.of(actor.id(), mosqueId, "MOSQUE_UPDATED", "MOSQUE", mosqueId));
        return view(actor, mosque);
    }

    /**
     * Only a platform administrator changes the status. Anything but {@link MosqueStatus#ACTIVE} hides the mosque
     * and all its content from public endpoints; {@link MosqueStatus#BLOCKED} also locks out its administrators.
     * Nothing is deleted.
     */
    @Transactional
    public MosqueView setStatus(AuthenticatedUser actor, UUID mosqueId, MosqueStatus status) {
        if (!actor.isPlatformAdmin()) {
            throw new MosqueAccessDeniedException();
        }
        Mosque mosque = load(mosqueId);
        if (mosque.getStatus() != status) {
            mosque.setStatus(status, Instant.now(clock));
            audit.publishEvent(AuditEvent.of(actor.id(), mosqueId, "MOSQUE_STATUS_" + status.name(), "MOSQUE",
                    mosqueId));
        }
        return view(actor, mosque);
    }

    private Mosque load(UUID mosqueId) {
        return mosques.findById(mosqueId).orElseThrow(MosqueNotFound::exception);
    }

    private MosqueView view(AuthenticatedUser actor, Mosque mosque) {
        MosqueRole role = administrators.findByMosqueIdAndUserId(mosque.getId(), actor.id())
                .map(MosqueAdministrator::getRole).orElse(null);
        return MosqueView.of(mosque, role);
    }

}
