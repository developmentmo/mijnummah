package nl.mijnummah.mosque;

import java.util.Set;
import java.util.UUID;

import nl.mijnummah.identity.AuthenticatedUser;

/**
 * Explicit, data-based authorization for everything a user does on behalf of a mosque. Every admin endpoint
 * calls {@link #require} with the mosque ID from its route before it touches any data; the mosque ID from a
 * client is therefore never trusted on its own.
 * <p>
 * Rules: a platform administrator may do everything for every mosque. Anyone else needs a
 * {@code mosque_administrator} row for exactly that mosque, whose {@link MosqueRole} grants the permission.
 */
public interface MosqueAccessService {

    /**
     * @throws nl.mijnummah.common.error.NotFoundException {@code MOSQUE_NOT_FOUND} when the mosque does not exist
     * @throws MosqueAccessDeniedException                 when the caller lacks the permission for this mosque
     */
    void require(AuthenticatedUser actor, UUID mosqueId, MosquePermission permission);

    /** Whether the caller has the permission for this mosque (false when the mosque does not exist). */
    boolean has(AuthenticatedUser actor, UUID mosqueId, MosquePermission permission);

    /** IDs of the mosques the caller administers (all mosques for a platform administrator). */
    Set<UUID> administeredMosqueIds(AuthenticatedUser actor);
}
