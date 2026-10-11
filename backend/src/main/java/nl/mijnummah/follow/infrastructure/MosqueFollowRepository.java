package nl.mijnummah.follow.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nl.mijnummah.follow.NotificationTopic;
import nl.mijnummah.follow.domain.MosqueFollow;

public interface MosqueFollowRepository extends JpaRepository<MosqueFollow, MosqueFollow.Key> {

    List<MosqueFollow> findByUserIdOrderByCreatedAt(UUID userId);

    /** Followers without a stored "off" for the topic (no row means on). */
    @Query("""
            select f.userId from MosqueFollow f
            where f.mosqueId = :mosqueId
              and not exists (select p from NotificationPreference p
                              where p.userId = f.userId and p.mosqueId = f.mosqueId
                                and p.topic = :topic and p.enabled = false)""")
    List<UUID> findUserIdsToNotify(@Param("mosqueId") UUID mosqueId, @Param("topic") NotificationTopic topic);
}
