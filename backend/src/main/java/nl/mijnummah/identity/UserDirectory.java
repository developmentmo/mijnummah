package nl.mijnummah.identity;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Read-only lookup of users for other modules. */
public interface UserDirectory {

    /** Active user with this email address (case-insensitive). */
    Optional<UserSummary> findActiveByEmail(String email);

    Map<UUID, UserSummary> findByIds(Collection<UUID> ids);
}
