package nl.mijnummah.identity.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nl.mijnummah.identity.PlatformRole;
import nl.mijnummah.identity.domain.User;
import nl.mijnummah.identity.domain.UserStatus;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPlatformRole(PlatformRole platformRole);

    /** {@code pattern}: lower-case LIKE pattern with escaped wildcards, or null for all; {@code status} null for all. */
    @Query("""
            select u from User u
            where (:pattern is null or lower(u.email) like :pattern escape '\\' or lower(u.displayName) like :pattern escape '\\')
              and (:status is null or u.status = :status)
            """)
    Page<User> search(@Param("pattern") String pattern, @Param("status") UserStatus status, Pageable pageable);

    @Query("select u.id from User u where u.id in :ids and u.status = :status")
    List<UUID> findIdsByIdInAndStatus(@Param("ids") Collection<UUID> ids, @Param("status") UserStatus status);
}
