package nl.mijnummah.mosque.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.identity.UserDirectory;
import nl.mijnummah.identity.UserSummary;
import nl.mijnummah.mosque.MosqueAccessService;
import nl.mijnummah.mosque.MosquePermission;
import nl.mijnummah.mosque.MosqueRole;
import nl.mijnummah.mosque.domain.MosqueAdministrator;
import nl.mijnummah.mosque.infrastructure.MosqueAdministratorRepository;

/** Who may manage a mosque. Requires {@link MosquePermission#MANAGE_ADMINISTRATORS} for that mosque. */
@Service
public class MosqueAdministratorService {

    private final MosqueAdministratorRepository administrators;
    private final MosqueAccessService access;
    private final UserDirectory userDirectory;
    private final Clock clock;

    public MosqueAdministratorService(MosqueAdministratorRepository administrators, MosqueAccessService access,
                                      UserDirectory userDirectory, Clock clock) {
        this.administrators = administrators;
        this.access = access;
        this.userDirectory = userDirectory;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AdministratorView> list(AuthenticatedUser actor, UUID mosqueId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_ADMINISTRATORS);
        List<MosqueAdministrator> rows = administrators.findByMosqueIdOrderByCreatedAt(mosqueId);
        Map<UUID, UserSummary> users = userDirectory.findByIds(rows.stream().map(MosqueAdministrator::getUserId).toList());
        return rows.stream().map(row -> view(row, users.get(row.getUserId()))).toList();
    }

    /** Grants an existing, active account a role for this mosque. */
    @Transactional
    public AdministratorView add(AuthenticatedUser actor, UUID mosqueId, String email, MosqueRole role) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_ADMINISTRATORS);
        UserSummary user = userDirectory.findActiveByEmail(email).orElseThrow(AdministratorUserNotFoundException::new);
        if (administrators.findByMosqueIdAndUserId(mosqueId, user.id()).isPresent()) {
            throw new AdministratorAlreadyExistsException();
        }
        MosqueAdministrator administrator = MosqueAdministrator.grant(mosqueId, user.id(), role, actor.id(),
                Instant.now(clock));
        try {
            administrators.saveAndFlush(administrator);
        } catch (DataIntegrityViolationException ex) {
            throw new AdministratorAlreadyExistsException();
        }
        return view(administrator, user);
    }

    /** Without an actor: only for the startup bootstrap. Idempotent. */
    @Transactional
    public void grantUnchecked(UUID mosqueId, UUID userId, MosqueRole role) {
        if (administrators.findByMosqueIdAndUserId(mosqueId, userId).isEmpty()) {
            administrators.save(MosqueAdministrator.grant(mosqueId, userId, role, null, Instant.now(clock)));
        }
    }

    @Transactional
    public AdministratorView changeRole(AuthenticatedUser actor, UUID mosqueId, UUID administratorId, MosqueRole role) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_ADMINISTRATORS);
        MosqueAdministrator administrator = load(mosqueId, administratorId);
        if (administrator.getRole() == MosqueRole.ADMIN && role != MosqueRole.ADMIN) {
            requireAnotherAdmin(mosqueId);
        }
        administrator.changeRole(role);
        return view(administrator, userDirectory.findByIds(List.of(administrator.getUserId()))
                .get(administrator.getUserId()));
    }

    @Transactional
    public void remove(AuthenticatedUser actor, UUID mosqueId, UUID administratorId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_ADMINISTRATORS);
        MosqueAdministrator administrator = load(mosqueId, administratorId);
        if (administrator.getRole() == MosqueRole.ADMIN) {
            requireAnotherAdmin(mosqueId);
        }
        administrators.delete(administrator);
    }

    /** Lookup by (mosque, id): an administrator ID of another mosque is "not found", never touched. */
    private MosqueAdministrator load(UUID mosqueId, UUID administratorId) {
        return administrators.findById(administratorId)
                .filter(administrator -> administrator.getMosqueId().equals(mosqueId))
                .orElseThrow(AdministratorNotFoundException::new);
    }

    private void requireAnotherAdmin(UUID mosqueId) {
        if (administrators.countByMosqueIdAndRole(mosqueId, MosqueRole.ADMIN) <= 1) {
            throw new LastAdminException();
        }
    }

    private static AdministratorView view(MosqueAdministrator row, UserSummary user) {
        return new AdministratorView(row.getId(), row.getUserId(), user == null ? null : user.email(),
                user == null ? null : user.displayName(), row.getRole(), row.getCreatedAt());
    }
}
