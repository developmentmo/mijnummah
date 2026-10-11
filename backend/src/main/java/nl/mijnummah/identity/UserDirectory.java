package nl.mijnummah.identity;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Read-only lookup of users for other modules. */
public interface UserDirectory {

    /** Active user with this email address (case-insensitive). */
    Optional<UserSummary> findActiveByEmail(String email);

    Map<UUID, UserSummary> findByIds(Collection<UUID> ids);

    /** The IDs among {@code ids} that belong to active (not blocked) accounts. */
    Set<UUID> activeAmong(Collection<UUID> ids);
}
