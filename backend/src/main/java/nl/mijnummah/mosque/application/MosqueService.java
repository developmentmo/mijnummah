package nl.mijnummah.mosque.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.mosque.MosqueAccessDeniedException;
import nl.mijnummah.mosque.MosqueAccessService;
import nl.mijnummah.mosque.MosquePermission;
import nl.mijnummah.mosque.MosqueRole;
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
    private final Clock clock;

    public MosqueService(MosqueRepository mosques, MosqueAdministratorRepository administrators,
                         MosqueAccessService access, Clock clock) {
        this.mosques = mosques;
        this.administrators = administrators;
        this.access = access;
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
        return MosqueView.of(createUnchecked(slug, details), null);
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

    /** The mosques the caller administers, with their own role. */
    @Transactional(readOnly = true)
    public List<MosqueView> administered(AuthenticatedUser actor) {
        Map<UUID, MosqueRole> roles = administrators.findByUserId(actor.id()).stream()
                .collect(Collectors.toMap(MosqueAdministrator::getMosqueId, MosqueAdministrator::getRole));
        List<Mosque> visible = actor.isPlatformAdmin()
                ? mosques.findAll()
                : mosques.findAllById(roles.keySet());
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
        return view(actor, mosque);
    }

    /** Deactivating hides the mosque and all its content from public endpoints; nothing is deleted. */
    @Transactional
    public MosqueView setActive(AuthenticatedUser actor, UUID mosqueId, boolean active) {
        if (!actor.isPlatformAdmin()) {
            throw new MosqueAccessDeniedException();
        }
        Mosque mosque = load(mosqueId);
        mosque.setActive(active, Instant.now(clock));
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
