package nl.mijnummah.follow.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import nl.mijnummah.follow.domain.NotificationPreference;

public interface NotificationPreferenceRepository
        extends JpaRepository<NotificationPreference, NotificationPreference.Key> {

    List<NotificationPreference> findByUserIdAndMosqueIdIn(UUID userId, Collection<UUID> mosqueIds);
}
