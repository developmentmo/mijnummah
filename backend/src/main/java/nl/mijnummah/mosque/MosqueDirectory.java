package nl.mijnummah.mosque;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Read-only lookup of mosques for other modules. */
public interface MosqueDirectory {

    /**
     * The active mosque with this slug, for public endpoints.
     *
     * @throws nl.mijnummah.common.error.NotFoundException {@code MOSQUE_NOT_FOUND} when unknown or inactive
     */
    MosqueRef requireActiveBySlug(String slug);

    /** @throws nl.mijnummah.common.error.NotFoundException {@code MOSQUE_NOT_FOUND} when unknown or inactive */
    MosqueRef requireActive(UUID mosqueId);

    /** Active mosques among the given IDs. */
    Map<UUID, MosqueRef> findActiveByIds(Collection<UUID> mosqueIds);

    /** Active mosques among the given slugs; unknown and inactive slugs are left out. */
    Map<UUID, MosqueRef> findActiveBySlugs(Collection<String> slugs);

    /** IDs of all active mosques, for public lists across mosques. */
    Set<UUID> activeIds();

    /** Names of the given mosques in any status, e.g. for the audit log; unknown IDs are left out. */
    Map<UUID, String> namesByIds(Collection<UUID> mosqueIds);

    /** The mosque's configuration; the defaults for an unknown mosque. */
    MosqueSettingsView settings(UUID mosqueId);
}
