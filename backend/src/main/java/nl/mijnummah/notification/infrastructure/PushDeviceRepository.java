package nl.mijnummah.notification.infrastructure;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nl.mijnummah.notification.domain.PushDevice;

public interface PushDeviceRepository extends JpaRepository<PushDevice, UUID> {

    Optional<PushDevice> findByToken(String token);

    /** Insert, or move an existing token to this user; atomic, so concurrent registrations cannot collide. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true, value = """
            insert into push_device (id, user_id, token, platform, created_at, last_seen_at, version)
            values (:id, :userId, :token, :platform, :now, :now, 0)
            on conflict (token) do update
                set user_id = excluded.user_id, platform = excluded.platform, last_seen_at = excluded.last_seen_at,
                    version = push_device.version + 1""")
    void upsert(@Param("id") UUID id, @Param("userId") UUID userId, @Param("token") String token,
                @Param("platform") String platform, @Param("now") Instant now);

    List<PushDevice> findByUserIdIn(Collection<UUID> userIds);

    long countByUserIdIn(Collection<UUID> userIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PushDevice d where d.token = :token and d.userId = :userId")
    int deleteByTokenAndUserId(@Param("token") String token, @Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PushDevice d where d.token in :tokens")
    int deleteByTokenIn(@Param("tokens") Collection<String> tokens);
}
