package nl.mijnummah.mosque;

import java.util.Collection;
import java.util.Map;
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
}
