package nl.mijnummah.mosque.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import nl.mijnummah.mosque.MosqueRole;
import nl.mijnummah.mosque.domain.MosqueAdministrator;

public interface MosqueAdministratorRepository extends JpaRepository<MosqueAdministrator, UUID> {

    Optional<MosqueAdministrator> findByMosqueIdAndUserId(UUID mosqueId, UUID userId);

    List<MosqueAdministrator> findByUserId(UUID userId);

    List<MosqueAdministrator> findByMosqueIdOrderByCreatedAt(UUID mosqueId);

    long countByMosqueIdAndRole(UUID mosqueId, MosqueRole role);
}
