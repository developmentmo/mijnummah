package nl.mijnummah.mosque.application;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.mosque.MosqueAccessDeniedException;
import nl.mijnummah.mosque.MosqueAccessService;
import nl.mijnummah.mosque.MosquePermission;
import nl.mijnummah.mosque.domain.Mosque;
import nl.mijnummah.mosque.domain.MosqueAdministrator;
import nl.mijnummah.mosque.infrastructure.MosqueAdministratorRepository;
import nl.mijnummah.mosque.infrastructure.MosqueRepository;

@Service
class DefaultMosqueAccessService implements MosqueAccessService {

    private final MosqueRepository mosques;
    private final MosqueAdministratorRepository administrators;

    DefaultMosqueAccessService(MosqueRepository mosques, MosqueAdministratorRepository administrators) {
        this.mosques = mosques;
        this.administrators = administrators;
    }

    @Override
    @Transactional(readOnly = true)
    public void require(AuthenticatedUser actor, UUID mosqueId, MosquePermission permission) {
        Mosque mosque = mosques.findById(mosqueId).orElseThrow(MosqueNotFound::exception);
        if (!granted(actor, mosque, permission)) {
            throw new MosqueAccessDeniedException();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean has(AuthenticatedUser actor, UUID mosqueId, MosquePermission permission) {
        return mosques.findById(mosqueId).map(mosque -> granted(actor, mosque, permission)).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> administeredMosqueIds(AuthenticatedUser actor) {
        if (actor.isPlatformAdmin()) {
            return mosques.findAll().stream().map(Mosque::getId).collect(Collectors.toUnmodifiableSet());
        }
        Set<UUID> ids = administrators.findByUserId(actor.id()).stream()
                .map(MosqueAdministrator::getMosqueId)
                .collect(Collectors.toUnmodifiableSet());
        return mosques.findAllById(ids).stream()
                .filter(mosque -> !mosque.isBlocked())
                .map(Mosque::getId)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** A platform administrator may do everything; nobody else may manage a blocked mosque. */
    private boolean granted(AuthenticatedUser actor, Mosque mosque, MosquePermission permission) {
        if (actor.isPlatformAdmin()) {
            return true;
        }
        if (mosque.isBlocked()) {
            return false;
        }
        return administrators.findByMosqueIdAndUserId(mosque.getId(), actor.id())
                .map(administrator -> administrator.getRole().grants(permission))
                .orElse(false);
    }
}
