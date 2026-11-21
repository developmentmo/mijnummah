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
import nl.mijnummah.identity.AccountProvisioning;
import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.mosque.MosqueAccessDeniedException;
import nl.mijnummah.mosque.MosqueAccessService;
import nl.mijnummah.mosque.MosquePermission;
import nl.mijnummah.mosque.MosqueRole;
import nl.mijnummah.mosque.MosqueSettingsView;
import nl.mijnummah.mosque.MosqueStatus;
import nl.mijnummah.mosque.domain.Mosque;
import nl.mijnummah.mosque.domain.MosqueAdministrator;
import nl.mijnummah.mosque.domain.MosqueDetails;
import nl.mijnummah.mosque.domain.MosqueSettings;
import nl.mijnummah.mosque.domain.Slugs;
import nl.mijnummah.mosque.infrastructure.MosqueAdministratorRepository;
import nl.mijnummah.mosque.infrastructure.MosqueRepository;
import nl.mijnummah.mosque.infrastructure.MosqueSettingsRepository;
import nl.mijnummah.storage.ImageStorage;
import nl.mijnummah.storage.StoredImageRef;

@Service
public class MosqueService {

    private final MosqueRepository mosques;
    private final MosqueAdministratorRepository administrators;
    private final MosqueSettingsRepository settings;
    private final MosqueAccessService access;
    private final AccountProvisioning accounts;
    private final ApplicationEventPublisher audit;
    private final ImageStorage images;
    private final Clock clock;

    public MosqueService(MosqueRepository mosques, MosqueAdministratorRepository administrators,
                         MosqueSettingsRepository settings, MosqueAccessService access, AccountProvisioning accounts,
                         ApplicationEventPublisher audit, ImageStorage images, Clock clock) {
        this.mosques = mosques;
        this.administrators = administrators;
        this.settings = settings;
        this.accounts = accounts;
        this.access = access;
        this.audit = audit;
        this.images = images;
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

    /**
     * Registers a mosque with its initial configuration, and optionally its first administrator (ADMIN). An empty
     * slug is made from the name. All of it happens in one transaction: if anything fails, nothing is created and
     * no invitation is sent.
     */
    @Transactional
    public CreatedMosque create(AuthenticatedUser actor, NewMosque request) {
        if (!actor.isPlatformAdmin()) {
            throw new MosqueAccessDeniedException();
        }
        String slug = request.slug() == null || request.slug().isBlank()
                ? Slugs.unique(Slugs.fromName(request.details().name()), mosques::existsBySlug)
                : request.slug();
        MosqueSettingsView initial = request.settings() == null ? MosqueSettingsView.DEFAULTS : request.settings();
        Mosque mosque = createUnchecked(slug, request.details(), initial);
        UUID mosqueId = mosque.getId();
        audit.publishEvent(AuditEvent.of(actor.id(), mosqueId, "MOSQUE_CREATED", "MOSQUE", mosqueId));
        AccountProvisioning.ProvisionedAccount administrator = null;
        if (request.firstAdministrator() != null) {
            administrator = accounts.findOrInvite(actor.id(), request.firstAdministrator().email(),
                    request.firstAdministrator().displayName());
            MosqueAdministrator row = administrators.save(MosqueAdministrator.grant(mosqueId, administrator.id(),
                    MosqueRole.ADMIN, actor.id(), Instant.now(clock)));
            audit.publishEvent(AuditEvent.of(actor.id(), mosqueId, "ADMINISTRATOR_ADDED", "ADMINISTRATOR",
                    row.getId()));
        }
        return new CreatedMosque(MosqueView.of(mosque, null), initial,
                administrator == null ? null : administrator.id(), administrator != null && administrator.invited());
    }

    /** What a platform administrator fills in for a new mosque; {@code slug}, admin and settings are optional. */
    public record NewMosque(String slug, MosqueDetails details, FirstAdministrator firstAdministrator,
                            MosqueSettingsView settings) {
    }

    public record FirstAdministrator(String email, String displayName) {
    }

    /** @param administratorInvited true when a new account was made and an invitation email will be sent */
    public record CreatedMosque(MosqueView mosque, MosqueSettingsView settings, UUID administratorUserId,
                                boolean administratorInvited) {
    }

    /** Without an actor: only for the startup bootstrap of the first mosque. */
    @Transactional
    public Mosque createUnchecked(String slug, MosqueDetails details) {
        return createUnchecked(slug, details, MosqueSettingsView.DEFAULTS);
    }

    private Mosque createUnchecked(String slug, MosqueDetails details, MosqueSettingsView initial) {
        if (mosques.existsBySlug(slug)) {
            throw new SlugAlreadyTakenException();
        }
        Instant now = Instant.now(clock);
        Mosque mosque = Mosque.create(slug, details, now);
        try {
            mosques.saveAndFlush(mosque);
        } catch (DataIntegrityViolationException ex) {
            throw new SlugAlreadyTakenException();
        }
        settings.save(MosqueSettings.create(mosque.getId(), initial.registrationsEnabled(), initial.pushEnabled(),
                now));
        return mosque;
    }

    /** Readable by everyone who manages the mosque, so the portal can explain why something is switched off. */
    @Transactional(readOnly = true)
    public MosqueSettingsView settings(AuthenticatedUser actor, UUID mosqueId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        return settings.findById(mosqueId).map(MosqueService::settingsView).orElse(MosqueSettingsView.DEFAULTS);
    }

    /** Only a platform administrator changes the configuration. */
    @Transactional
    public MosqueSettingsView updateSettings(AuthenticatedUser actor, UUID mosqueId, MosqueSettingsView changed) {
        if (!actor.isPlatformAdmin()) {
            throw new MosqueAccessDeniedException();
        }
        load(mosqueId);
        Instant now = Instant.now(clock);
        MosqueSettings row = settings.findById(mosqueId)
                .orElseGet(() -> MosqueSettings.create(mosqueId, true, true, now));
        row.change(changed.registrationsEnabled(), changed.pushEnabled(), now);
        settings.save(row);
        audit.publishEvent(AuditEvent.of(actor.id(), mosqueId, "MOSQUE_SETTINGS_UPDATED", "MOSQUE", mosqueId));
        return settingsView(row);
    }

    static MosqueSettingsView settingsView(MosqueSettings row) {
        return new MosqueSettingsView(row.isRegistrationsEnabled(), row.isPushEnabled());
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

    /** Uploads an image for this mosque's content or profile; it is only shown once something refers to it. */
    @Transactional
    public StoredImageRef uploadImage(AuthenticatedUser actor, UUID mosqueId, byte[] content) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        return images.store(mosqueId, actor.id(), content);
    }

    /** Sets or removes (null) the logo and cover photo, which must be images uploaded for this mosque. */
    @Transactional
    public MosqueView setImages(AuthenticatedUser actor, UUID mosqueId, UUID logoImageId, UUID coverImageId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_MOSQUE);
        if (logoImageId != null) {
            images.requireOwnedBy(logoImageId, mosqueId);
        }
        if (coverImageId != null) {
            images.requireOwnedBy(coverImageId, mosqueId);
        }
        Mosque mosque = load(mosqueId);
        mosque.setImages(logoImageId, coverImageId, Instant.now(clock));
        audit.publishEvent(AuditEvent.of(actor.id(), mosqueId, "MOSQUE_IMAGES_UPDATED", "MOSQUE", mosqueId));
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
