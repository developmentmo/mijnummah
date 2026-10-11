package nl.mijnummah.identity.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import nl.mijnummah.identity.PlatformRole;
import nl.mijnummah.identity.domain.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPlatformRole(PlatformRole platformRole);
}
